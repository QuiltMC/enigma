package org.quiltmc.enigma.impl.analysis.index;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndexer;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.Signature;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.impl.analysis.IndexSimpleVerifier;
import org.quiltmc.enigma.impl.analysis.MethodNodeWithAction;

import java.util.List;

public class IndexReferenceVisitor extends ClassVisitor {
	private final JarIndexer indexer;
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private ClassEntry classEntry;
	private String className;

	public IndexReferenceVisitor(JarIndexer indexer, EntryIndex entryIndex, InheritanceIndex inheritanceIndex, int api) {
		super(api);
		this.indexer = indexer;
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = new ClassEntry(name);
		this.className = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(this.classEntry, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		return new MethodNodeWithAction(this.api, access, name, desc, signature, exceptions, methodNode -> {
			try {
				new Analyzer<>(new MethodInterpreter(entry, this.indexer, this.entryIndex, this.inheritanceIndex)).analyze(this.className, methodNode);
			} catch (AnalyzerException e) {
				throw new RuntimeException("Failed to analyze " + methodNode.name, e);
			}
		});
	}

	// TODO: IndexSimpleVerifier might have issues
	private static class MethodInterpreter extends IndexSimpleVerifier {
		private final MethodDefEntry callerEntry;
		private final JarIndexer indexer;

		MethodInterpreter(MethodDefEntry callerEntry, JarIndexer indexer, EntryIndex entryIndex, InheritanceIndex inheritanceIndex) {
			super(entryIndex, inheritanceIndex);
			this.callerEntry = callerEntry;
			this.indexer = indexer;
		}

		@Override
		public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.GETSTATIC) {
				FieldInsnNode field = (FieldInsnNode) insn;
				this.indexer.indexFieldReference(this.callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.LDC) {
				LdcInsnNode ldc = (LdcInsnNode) insn;

				if (ldc.getType() == Type.ARRAY && ldc.cst instanceof Type type) {
					String className = type.getClassName().replace(".", "/");
					this.indexer.indexClassReference(this.callerEntry, new ClassEntry(className), ReferenceTargetType.none());
				}
			}

			return super.newOperation(insn);
		}

		@Override
		public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.PUTSTATIC) {
				FieldInsnNode field = (FieldInsnNode) insn;
				this.indexer.indexFieldReference(this.callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.GETFIELD) {
				FieldInsnNode field = (FieldInsnNode) insn;
				this.indexer.indexFieldReference(this.callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), this.getReferenceTargetType(value, insn));
			}

			// Note: type.desc is actually the name

			if (insn.getOpcode() == Opcodes.INSTANCEOF) {
				TypeInsnNode type = (TypeInsnNode) insn;
				this.indexer.indexClassReference(this.callerEntry, new ClassEntry(type.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.CHECKCAST) {
				TypeInsnNode type = (TypeInsnNode) insn;
				this.indexer.indexClassReference(this.callerEntry, new ClassEntry(type.desc), ReferenceTargetType.none());
			}

			return super.unaryOperation(insn, value);
		}

		@Override
		public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.PUTFIELD) {
				FieldInsnNode field = (FieldInsnNode) insn;
				FieldEntry fieldEntry = FieldEntry.parse(field.owner, field.name, field.desc);
				this.indexer.indexFieldReference(this.callerEntry, fieldEntry, ReferenceTargetType.none());
			}

			return super.binaryOperation(insn, value1, value2);
		}

		@Override
		public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.INVOKEINTERFACE || insn.getOpcode() == Opcodes.INVOKESPECIAL || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				this.indexer.indexMethodReference(this.callerEntry, MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc), this.getReferenceTargetType(values.get(0), insn));
			}

			if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				this.indexer.indexMethodReference(this.callerEntry, MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
				InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;

				if ("java/lang/invoke/LambdaMetafactory".equals(invokeDynamicInsn.bsm.getOwner()) && "metafactory".equals(invokeDynamicInsn.bsm.getName())) {
					Type samMethodType = (Type) invokeDynamicInsn.bsmArgs[0];
					Handle implMethod = (Handle) invokeDynamicInsn.bsmArgs[1];
					Type instantiatedMethodType = (Type) invokeDynamicInsn.bsmArgs[2];

					ReferenceTargetType targetType;
					if (implMethod.getTag() != Opcodes.H_GETSTATIC && implMethod.getTag() != Opcodes.H_PUTFIELD && implMethod.getTag() != Opcodes.H_INVOKESTATIC) {
						if (instantiatedMethodType.getArgumentTypes().length < Type.getArgumentTypes(implMethod.getDesc()).length) {
							targetType = this.getReferenceTargetType(values.get(0), insn);
						} else {
							targetType = ReferenceTargetType.none(); // no "this" argument
						}
					} else {
						targetType = ReferenceTargetType.none();
					}

					this.indexer.indexLambda(this.callerEntry, new Lambda(
							invokeDynamicInsn.name,
							new MethodDescriptor(invokeDynamicInsn.desc),
							new MethodDescriptor(samMethodType.getDescriptor()),
							getHandleEntry(implMethod),
							new MethodDescriptor(instantiatedMethodType.getDescriptor())
					), targetType);
				}
			}

			return super.naryOperation(insn, values);
		}

		private ReferenceTargetType getReferenceTargetType(BasicValue target, AbstractInsnNode insn) throws AnalyzerException {
			if (target == BasicValue.UNINITIALIZED_VALUE) {
				return ReferenceTargetType.uninitialized();
			}

			if (target.getType().getSort() == Type.OBJECT) {
				return ReferenceTargetType.classType(new ClassEntry(target.getType().getInternalName()));
			}

			if (target.getType().getSort() == Type.ARRAY) {
				return ReferenceTargetType.classType(new ClassEntry("java/lang/Object"));
			}

			throw new AnalyzerException(insn, "called method on or accessed field of non-object type");
		}

		private static ParentedEntry<?> getHandleEntry(Handle handle) {
			switch (handle.getTag()) {
				case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> {
					return FieldEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
				}
				case Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESPECIAL, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKEVIRTUAL, Opcodes.H_NEWINVOKESPECIAL -> {
					return MethodEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
				}
			}

			throw new RuntimeException("Invalid handle tag " + handle.getTag());
		}
	}
}

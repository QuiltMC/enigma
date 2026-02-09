package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.quiltmc.enigma.api.Enigma;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ParamSyntheticFieldIndexingVisitor extends ClassVisitor {
	final Map<String, Multimap<MethodNode, TypeInstructionIndex>> localTypeInstructionsByMethodByOwner = new HashMap<>();
	// excludes no-args constructors
	final Map<String, Map<String, MethodNode>> localConstructorsByDescByOwner = new HashMap<>();
	final Multimap<String, FieldNode> localSyntheticFieldsByOwner = HashMultimap.create();

	private String className;
	private boolean classIsLocal;
	private final List<MethodNode> methods = new LinkedList<>();
	private final Set<String> localClassNames = new HashSet<>();

	ParamSyntheticFieldIndexingVisitor() {
		super(Enigma.ASM_VERSION);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (innerName == null) {
			if (this.className.equals(name)) {
				this.classIsLocal = true;
			} else {
				this.localClassNames.add(name);
			}
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (this.classIsLocal && (access & Opcodes.ACC_SYNTHETIC) != 0 && (access & Opcodes.ACC_STATIC) == 0) {
			final var node = new FieldNode(access, name, descriptor, signature, value);
			this.localSyntheticFieldsByOwner.put(this.className, node);

			return node;
		} else {
			return super.visitField(access, name, descriptor, signature, value);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		final boolean hasLocalClasses = !this.localClassNames.isEmpty();
		if (this.classIsLocal || hasLocalClasses) {
			final MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);

			if (this.classIsLocal && name.equals("<init>") && !descriptor.startsWith("()")) {
				this.localConstructorsByDescByOwner
						.computeIfAbsent(this.className, owner -> new HashMap<>())
						.put(descriptor, node);
			}

			if (hasLocalClasses) {
				this.methods.add(node);
			}

			return node;
		} else {
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		this.collectResults();

		this.className = null;
		this.classIsLocal = false;
		this.methods.clear();
		this.localClassNames.clear();
	}

	private void collectResults() {
		for (final MethodNode method : this.methods) {
			AbstractInsnNode instruction = method.instructions.getFirst();
			int index = 0;
			while (instruction != null) {
				if (
						instruction instanceof TypeInsnNode typeInstruction
							&& typeInstruction.getOpcode() == Opcodes.NEW
							&& this.localClassNames.contains(typeInstruction.desc)
				) {
					this.localTypeInstructionsByMethodByOwner
							.computeIfAbsent(this.className, owner -> HashMultimap.create())
							.put(method, new TypeInstructionIndex(typeInstruction, index));
				}

				instruction = instruction.getNext();
				index++;
			}
		}
	}

	record TypeInstructionIndex(TypeInsnNode typeInstruction, int index) { }
}

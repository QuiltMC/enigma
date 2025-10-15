package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

final class RecordIndexingVisitor extends ClassVisitor {
	private ClassEntry clazz;
	private final Set<RecordComponentNode> recordComponents = new HashSet<>();
	private final Set<FieldNode> fields = new HashSet<>();
	private final Set<MethodNode> methods = new HashSet<>();

	private final BiMap<FieldEntry, MethodEntry> gettersByField;
	private final Multimap<ClassEntry, FieldEntry> fieldsByClass = HashMultimap.create();
	private final Multimap<ClassEntry, MethodEntry> methodsByClass = HashMultimap.create();

	RecordIndexingVisitor() {
		super(Enigma.ASM_VERSION);
		this.gettersByField = HashBiMap.create();
	}

	@Nullable
	public MethodEntry getComponentGetter(FieldEntry componentField) {
		return this.gettersByField.get(componentField);
	}

	@Nullable
	public FieldEntry getComponentField(MethodEntry componentGetter) {
		return this.gettersByField.inverse().get(componentGetter);
	}

	public Stream<FieldEntry> streamComponentFields(ClassEntry recordEntry) {
		return this.fieldsByClass.get(recordEntry).stream();
	}

	public Stream<MethodEntry> streamComponentMethods(ClassEntry recordEntry) {
		return this.methodsByClass.get(recordEntry).stream();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.clazz = (access & Opcodes.ACC_RECORD) != 0 ? new ClassEntry(name) : null;
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(final String name, final String descriptor, final String signature) {
		this.recordComponents.add(new RecordComponentNode(this.api, name, descriptor, signature));
		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
		if (this.clazz != null && ((access & Opcodes.ACC_PRIVATE) != 0) && this.recordComponents.stream().anyMatch(component -> component.name.equals(name))) {
			FieldNode node = new FieldNode(this.api, access, name, descriptor, signature, value);
			this.fields.add(node);
			return node;
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		if (this.clazz != null && ((access & Opcodes.ACC_PUBLIC) != 0)) {
			MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
			this.methods.add(node);
			return node;
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		try {
			this.collectResults();

			this.clazz = null;
			this.recordComponents.clear();
			this.fields.clear();
			this.methods.clear();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void collectResults() {
		if (this.clazz == null) {
			return;
		}

		for (RecordComponentNode component : this.recordComponents) {
			FieldNode field = null;
			for (FieldNode node : this.fields) {
				if (node.name.equals(component.name) && node.desc.equals(component.descriptor)) {
					field = node;
					break;
				}
			}

			if (field == null) {
				throw new RuntimeException("Field not found for record component: " + component.name);
			}

			for (MethodNode method : this.methods) {
				InsnList instructions = method.instructions;

				// match bytecode to exact expected bytecode for a getter
				// only check important instructions (ignore new frame instructions, etc.)
				if (
						instructions.size() == 6
							&& instructions.get(2).getOpcode() == Opcodes.ALOAD
							&& instructions.get(3) instanceof FieldInsnNode fieldInsn
							&& fieldInsn.getOpcode() == Opcodes.GETFIELD
							&& fieldInsn.owner.equals(this.clazz.getFullName())
							&& fieldInsn.desc.equals(field.desc)
							&& fieldInsn.name.equals(field.name)
							&& instructions.get(4).getOpcode() >= Opcodes.IRETURN
							&& instructions.get(4).getOpcode() <= Opcodes.ARETURN
				) {
					final FieldEntry fieldEntry = new FieldEntry(this.clazz, field.name, new TypeDescriptor(field.desc));
					final MethodEntry methodEntry = new MethodEntry(this.clazz, method.name, new MethodDescriptor(method.desc));

					this.gettersByField.put(fieldEntry, methodEntry);
					this.fieldsByClass.put(this.clazz, fieldEntry);
					this.methodsByClass.put(this.clazz, methodEntry);
				}
			}
		}
	}
}


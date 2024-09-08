package org.quiltmc.enigma.impl.plugin;

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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// todo may be getting duplicate methods/fields?
final class RecordGetterFindingVisitor extends ClassVisitor {
	private ClassEntry clazz;
	private final Map<FieldEntry, MethodEntry> fieldToMethod;
	private final Set<RecordComponentNode> recordComponents = new HashSet<>();
	private final Set<FieldNode> fields = new HashSet<>();
	private final Set<MethodNode> methods = new HashSet<>();

	RecordGetterFindingVisitor(Map<FieldEntry, MethodEntry> fieldToMethod) {
		super(Enigma.ASM_VERSION);
		this.fieldToMethod = fieldToMethod;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.clazz = (access & Opcodes.ACC_RECORD) != 0 ? new ClassEntry(name) : null;
		this.recordComponents.clear();
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(final String name, final String descriptor, final String signature) {
		this.recordComponents.add(new RecordComponentNode(this.api, name, descriptor, signature));
		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
		if (this.clazz != null) {
			FieldNode node = new FieldNode(this.api, access, name, descriptor, signature, value);
			this.fields.add(node);
			return node;
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		if (this.clazz != null) {
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
			if (this.clazz != null) {
				this.collectResults();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void collectResults() {
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
				System.out.println(instructions.toString());

				// todo is this stupid
				if (instructions.size() == 6
						&& instructions.get(2).getOpcode() == Opcodes.ALOAD
						&& instructions.get(3) instanceof FieldInsnNode fieldInsn
						&& fieldInsn.getOpcode() == Opcodes.GETFIELD
						&& fieldInsn.owner.equals(this.clazz.getName())
						&& fieldInsn.desc.equals(field.desc)
						&& fieldInsn.name.equals(field.name)
						&& instructions.get(4).getOpcode() == Opcodes.IRETURN) { // todo match to ALL return opcodes
					this.fieldToMethod.put(new FieldEntry(this.clazz, field.name, new TypeDescriptor(field.desc)), new MethodEntry(this.clazz, method.name, new MethodDescriptor(method.desc)));
				}

//				instructions.get(0); // opcode -1 label node
//				instructions.get(1); // opcode -1 line number node
//				instructions.get(2); // opcode 25 var insn node
//				instructions.get(3); // opcode 180 field insn node (gets record field)
//				instructions.get(4); // opcode 172 insn node
//				instructions.get(5); // opcode -1 label node
//				boolean getField = instructions.get(3).getOpcode() == Opcodes.H_GETFIELD;
			}
		}
	}
}


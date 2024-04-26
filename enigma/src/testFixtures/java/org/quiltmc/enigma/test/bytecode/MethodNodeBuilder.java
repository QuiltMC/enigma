package org.quiltmc.enigma.test.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma.api.Enigma;

public final class MethodNodeBuilder {
	private final MethodNode methodNode;

	private MethodNodeBuilder(int access, String name, String descriptor, String signature, String[] exceptions) {
		this.methodNode = new MethodNode(Enigma.ASM_VERSION, access, name, descriptor, signature, exceptions);
		this.methodNode.maxLocals = 10;
		this.methodNode.maxStack = 10;
		this.methodNode.visitCode();
	}

	public static MethodNodeBuilder create(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodNodeBuilder(access, name, descriptor, signature, exceptions);
	}

	public static MethodNodeBuilder create(String name, String descriptor) {
		return new MethodNodeBuilder(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
	}

	public MethodNodeBuilder insn(int opcode) {
		this.methodNode.visitInsn(opcode);
		return this;
	}

	public MethodNodeBuilder iconst_0() {
		this.methodNode.visitInsn(Opcodes.ICONST_0);
		return this;
	}

	public MethodNodeBuilder aload(int var) {
		this.methodNode.visitVarInsn(Opcodes.ALOAD, var);
		return this;
	}

	public MethodNodeBuilder typeInsn(int opcode, String type) {
		this.methodNode.visitTypeInsn(opcode, type);
		return this;
	}

	public MethodNodeBuilder methodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		this.methodNode.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		return this;
	}

	public MethodNodeBuilder maxs(int maxLocals, int maxStack) {
		this.methodNode.maxLocals = maxLocals;
		this.methodNode.maxStack = maxStack;
		return this;
	}

	public MethodNode build() {
		this.methodNode.visitEnd();
		return this.methodNode;
	}
}

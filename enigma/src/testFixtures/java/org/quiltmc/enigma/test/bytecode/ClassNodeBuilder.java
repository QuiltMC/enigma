package org.quiltmc.enigma.test.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma.api.Enigma;

public final class ClassNodeBuilder {
	private final ClassNode classNode;

	private ClassNodeBuilder(int access, String name, String signature, String superName, String[] interfaces) {
		this.classNode = new ClassNode(Enigma.ASM_VERSION);
		this.classNode.visit(Opcodes.V1_8, access, name, signature, superName, interfaces);
	}

	public static ClassNodeBuilder create(int access, String name, String signature, String superName, String[] interfaces) {
		return new ClassNodeBuilder(access, name, signature, superName, interfaces);
	}

	public static ClassNodeBuilder create(String name, String superName) {
		return create(Opcodes.ACC_PUBLIC, name, null, superName, null);
	}

	public static ClassNodeBuilder create(String name) {
		return create(name, "java/lang/Object");
	}

	public ClassNodeBuilder field(int access, String name, String descriptor, String signature, Object value) {
		this.classNode.visitField(access, name, descriptor, signature, value);
		return this;
	}

	public ClassNodeBuilder field(int access, String name, String descriptor) {
		return this.field(access, name, descriptor, null, null);
	}

	public ClassNodeBuilder field(String name, String descriptor) {
		return this.field(Opcodes.ACC_PUBLIC, name, descriptor);
	}

	public ClassNodeBuilder method(MethodNode method) {
		method.accept(this.classNode);
		return this;
	}

	public ClassNodeBuilder superInit() {
		this.method(MethodNodeBuilder.create("<init>", "()V")
				.aload(0)
				.methodInsn(Opcodes.INVOKESPECIAL, this.classNode.superName, "<init>", "()V", false)
				.insn(Opcodes.RETURN)
				.maxs(1, 1)
				.build());
		return this;
	}

	public ClassNode build() {
		return this.classNode;
	}
}

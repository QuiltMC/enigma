package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

public class TranslationMethodVisitor extends MethodVisitor {
	private final MethodDefEntry methodEntry;
	private final Translator translator;

	private int parameterIndex = 0;
	private int parameterLvIndex;

	public TranslationMethodVisitor(Translator translator, ClassDefEntry ownerEntry, MethodDefEntry methodEntry, int api, MethodVisitor mv) {
		super(api, mv);
		this.translator = translator;
		this.methodEntry = methodEntry;

		this.parameterLvIndex = methodEntry.getAccess().isStatic() ? 0 : 1;
	}

	@Override
	public void visitParameter(String name, int access) {
		name = this.translateVariableName(this.parameterLvIndex, name);
		this.parameterLvIndex += this.methodEntry.getDesc().getArgumentDescs().get(this.parameterIndex++).getSize();

		super.visitParameter(name, access);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		FieldEntry entry = new FieldEntry(new ClassEntry(owner), name, new TypeDescriptor(desc));
		FieldEntry translatedEntry = this.translator.translate(entry);
		super.visitFieldInsn(opcode, translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		MethodEntry entry = new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc));
		MethodEntry translatedEntry = this.translator.translate(entry);
		super.visitMethodInsn(opcode, translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString(), itf);
	}

	@Override
	public void visitFrame(int type, int localCount, Object[] locals, int stackCount, Object[] stack) {
		Object[] translatedLocals = this.getTranslatedFrame(locals, localCount);
		Object[] translatedStack = this.getTranslatedFrame(stack, stackCount);
		super.visitFrame(type, localCount, translatedLocals, stackCount, translatedStack);
	}

	private Object[] getTranslatedFrame(Object[] array, int count) {
		if (array == null) {
			return null;
		}

		for (int i = 0; i < count; i++) {
			Object object = array[i];
			if (object instanceof String type) {
				array[i] = this.translator.translate(new ClassEntry(type)).getFullName();
			}
		}

		return array;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor typeDesc = this.translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(this.translator, typeDesc.getTypeEntry(), this.api, av);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		TypeDescriptor typeDesc = this.translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitParameterAnnotation(parameter, typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(this.translator, typeDesc.getTypeEntry(), this.api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor typeDesc = this.translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(this.translator, typeDesc.getTypeEntry(), this.api, av);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		ClassEntry translatedEntry = this.translator.translate(new ClassEntry(type));
		super.visitTypeInsn(opcode, translatedEntry.getFullName());
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		MethodDescriptor translatedMethodDesc = this.translator.translate(new MethodDescriptor(desc));
		Object[] translatedBsmArgs = new Object[bsmArgs.length];
		for (int i = 0; i < bsmArgs.length; i++) {
			translatedBsmArgs[i] = AsmObjectTranslator.translateValue(this.translator, bsmArgs[i]);
		}

		super.visitInvokeDynamicInsn(name, translatedMethodDesc.toString(), AsmObjectTranslator.translateHandle(this.translator, bsm), translatedBsmArgs);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(AsmObjectTranslator.translateValue(this.translator, cst));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		super.visitMultiANewArrayInsn(this.translator.translate(new TypeDescriptor(desc)).toString(), dims);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (type != null) {
			ClassEntry translatedEntry = this.translator.translate(new ClassEntry(type));
			super.visitTryCatchBlock(start, end, handler, translatedEntry.getFullName());
		} else {
			super.visitTryCatchBlock(start, end, handler, type);
		}
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		signature = this.translator.translate(Signature.createTypedSignature(signature)).toString();
		name = this.translateVariableName(index, name);
		desc = this.translator.translate(new TypeDescriptor(desc)).toString();

		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	private String translateVariableName(int index, String name) {
		LocalVariableEntry entry = new LocalVariableEntry(this.methodEntry, index, "", true, null);
		LocalVariableEntry translatedEntry = this.translator.translate(entry);
		String translatedName = translatedEntry.getName();

		if (!translatedName.isEmpty()) {
			return translatedName;
		}

		return name;
	}
}

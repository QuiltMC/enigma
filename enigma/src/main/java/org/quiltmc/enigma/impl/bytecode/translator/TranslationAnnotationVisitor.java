package org.quiltmc.enigma.impl.bytecode.translator;

import org.objectweb.asm.AnnotationVisitor;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;

public class TranslationAnnotationVisitor extends AnnotationVisitor {
	private final Translator translator;
	private final ClassEntry annotationEntry;

	public TranslationAnnotationVisitor(Translator translator, ClassEntry annotationEntry, int api, AnnotationVisitor av) {
		super(api, av);
		this.translator = translator;
		this.annotationEntry = annotationEntry;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, AsmObjectTranslator.translateValue(this.translator, value));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return new TranslationAnnotationVisitor(this.translator, this.annotationEntry, this.api, super.visitArray(name));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		TypeDescriptor type = new TypeDescriptor(desc);
		if (name != null) {
			FieldEntry annotationField = this.translator.translate(new FieldEntry(this.annotationEntry, name, type));
			return super.visitAnnotation(annotationField.getName(), annotationField.getDesc().toString());
		} else {
			return super.visitAnnotation(null, this.translator.translate(type).toString());
		}
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		TypeDescriptor type = new TypeDescriptor(desc);
		FieldEntry enumField = this.translator.translate(new FieldEntry(type.getTypeEntry(), value, type));
		if (name != null) {
			FieldEntry annotationField = this.translator.translate(new FieldEntry(this.annotationEntry, name, type));
			super.visitEnum(annotationField.getName(), annotationField.getDesc().toString(), enumField.getName());
		} else {
			super.visitEnum(null, this.translator.translate(type).toString(), enumField.getName());
		}
	}
}

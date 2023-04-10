package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

import java.util.Arrays;

public class TranslationClassVisitor extends ClassVisitor {
	private final Translator translator;

	private ClassDefEntry obfClassEntry;

	public TranslationClassVisitor(Translator translator, int api, ClassVisitor cv) {
		super(api, cv);
		this.translator = translator;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.obfClassEntry = ClassDefEntry.parse(access, name, signature, superName, interfaces);

		ClassDefEntry translatedEntry = this.translator.translate(this.obfClassEntry);
		String translatedSuper = translatedEntry.getSuperClass() != null ? translatedEntry.getSuperClass().getFullName() : null;
		String[] translatedInterfaces = Arrays.stream(translatedEntry.getInterfaces()).map(ClassEntry::getFullName).toArray(String[]::new);

		super.visit(version, translatedEntry.getAccess().getFlags(), translatedEntry.getFullName(), translatedEntry.getSignature().toString(), translatedSuper, translatedInterfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldDefEntry entry = FieldDefEntry.parse(this.obfClassEntry, access, name, desc, signature);
		FieldDefEntry translatedEntry = this.translator.translate(entry);
		FieldVisitor fv = super.visitField(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedEntry.getSignature().toString(), value);
		return new TranslationFieldVisitor(this.translator, translatedEntry, this.api, fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = MethodDefEntry.parse(this.obfClassEntry, access, name, desc, signature);
		MethodDefEntry translatedEntry = this.translator.translate(entry);
		String[] translatedExceptions = new String[exceptions.length];
		for (int i = 0; i < exceptions.length; i++) {
			translatedExceptions[i] = this.translator.translate(new ClassEntry(exceptions[i])).getFullName();
		}

		MethodVisitor mv = super.visitMethod(translatedEntry.getAccess().getFlags(), translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedEntry.getSignature().toString(), translatedExceptions);
		return new TranslationMethodVisitor(this.translator, this.obfClassEntry, entry, this.api, mv);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		ClassDefEntry classEntry = ClassDefEntry.parse(access, name, this.obfClassEntry.getSignature().toString(), null, new String[0]);
		ClassDefEntry translatedEntry = this.translator.translate(classEntry);
		ClassEntry translatedOuterClass = translatedEntry.getOuterClass();
		if (translatedOuterClass == null) {
			throw new IllegalStateException("Translated inner class did not have outer class");
		}

		// Anonymous classes do not specify an outer or inner name. As we do not translate from the given parameter, ignore if the input is null
		String translatedName = translatedEntry.getFullName();
		String translatedOuterName = outerName != null ? translatedOuterClass.getFullName() : null;
		String translatedInnerName = innerName != null ? translatedEntry.getName() : null;
		super.visitInnerClass(translatedName, translatedOuterName, translatedInnerName, translatedEntry.getAccess().getFlags());
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if (desc != null) {
			MethodEntry translatedEntry = this.translator.translate(new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc)));
			super.visitOuterClass(translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
		} else {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor translatedDesc = this.translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(this.translator, translatedDesc.getTypeEntry(), this.api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor translatedDesc = this.translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, translatedDesc.toString(), visible);
		return new TranslationAnnotationVisitor(this.translator, translatedDesc.getTypeEntry(), this.api, av);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String desc, String signature) {
		// Record component names are remapped via the field mapping.
		FieldDefEntry entry = FieldDefEntry.parse(this.obfClassEntry, 0, name, desc, signature);
		FieldDefEntry translatedEntry = this.translator.translate(entry);
		RecordComponentVisitor fv = super.visitRecordComponent(translatedEntry.getName(), translatedEntry.getDesc().toString(), translatedEntry.getSignature().toString());
		return new TranslationRecordComponentVisitor(this.translator, this.api, fv);
	}
}

package org.quiltmc.enigma.api.translation.representation.entry;

import com.google.common.base.Preconditions;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.Signature;

import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClassDefEntry extends ClassEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;
	@Nullable
	private final ClassEntry superClass;
	private final ClassEntry[] interfaces;

	public ClassDefEntry(String className, Signature signature, AccessFlags access, @Nullable ClassEntry superClass, ClassEntry[] interfaces) {
		this(getOuterClass(className), getInnerName(className), signature, access, superClass, interfaces, null);
	}

	public ClassDefEntry(ClassEntry parent, String className, Signature signature, AccessFlags access, @Nullable ClassEntry superClass, ClassEntry[] interfaces) {
		this(parent, className, signature, access, superClass, interfaces, null);
	}

	public ClassDefEntry(ClassEntry parent, String className, Signature signature, AccessFlags access, @Nullable ClassEntry superClass, ClassEntry[] interfaces, String javadocs) {
		super(parent, className, javadocs);
		Preconditions.checkNotNull(signature, "Class signature cannot be null");
		Preconditions.checkNotNull(access, "Class access cannot be null");

		this.signature = signature;
		this.access = access;
		this.superClass = superClass;
		this.interfaces = interfaces != null ? interfaces : new ClassEntry[0];
	}

	public static ClassDefEntry parse(int access, String name, String signature, String superName, String[] interfaces) {
		ClassEntry superClass = superName != null ? new ClassEntry(superName) : null;
		ClassEntry[] interfaceClasses = Arrays.stream(interfaces).map(ClassEntry::new).toArray(ClassEntry[]::new);
		return new ClassDefEntry(name, Signature.createSignature(signature), new AccessFlags(access), superClass, interfaceClasses);
	}

	public Signature getSignature() {
		return this.signature;
	}

	@Override
	public AccessFlags getAccess() {
		return this.access;
	}

	@Nullable
	public ClassEntry getSuperClass() {
		return this.superClass;
	}

	public ClassEntry[] getInterfaces() {
		return this.interfaces;
	}

	public boolean isEnum() {
		return this.superClass != null && this.superClass.getName().equals("java/lang/Enum");
	}

	public boolean isRecord() {
		return this.superClass != null && this.superClass.getName().equals("java/lang/Record");
	}

	@Override
	public TranslateResult<ClassDefEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		Signature translatedSignature = translator.translate(this.signature);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		ClassEntry translatedSuper = translator.translate(this.superClass);
		ClassEntry[] translatedInterfaces = Arrays.stream(this.interfaces).map(translator::translate).toArray(ClassEntry[]::new);
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping,
				new ClassDefEntry(this.parent, translatedName, translatedSignature, this.access, translatedSuper, translatedInterfaces, docs)
		);
	}

	@Override
	public ClassDefEntry withName(String name) {
		return new ClassDefEntry(this.parent, name, this.signature, this.access, this.superClass, this.interfaces, this.javadocs);
	}

	@Override
	public ClassDefEntry withParent(ClassEntry parent) {
		return new ClassDefEntry(parent, this.name, this.signature, this.access, this.superClass, this.interfaces, this.javadocs);
	}
}

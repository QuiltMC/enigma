package org.quiltmc.enigma.api.translation.representation.entry;

import com.google.common.base.Preconditions;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.Signature;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;

import javax.annotation.Nonnull;

public class FieldDefEntry extends FieldEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public FieldDefEntry(ClassEntry owner, String name, TypeDescriptor desc, Signature signature, AccessFlags access) {
		this(owner, name, desc, signature, access, null);
	}

	public FieldDefEntry(ClassEntry owner, String name, TypeDescriptor desc, Signature signature, AccessFlags access, String javadocs) {
		super(owner, name, desc, javadocs);
		Preconditions.checkNotNull(access, "Field access cannot be null");
		Preconditions.checkNotNull(signature, "Field signature cannot be null");
		this.access = access;
		this.signature = signature;
	}

	public static FieldDefEntry parse(ClassEntry owner, int access, String name, String desc, String signature) {
		return new FieldDefEntry(owner, name, new TypeDescriptor(desc), Signature.createTypedSignature(signature), new AccessFlags(access), null);
	}

	@Override
	public AccessFlags getAccess() {
		return this.access;
	}

	public Signature getSignature() {
		return this.signature;
	}

	@Override
	protected TranslateResult<FieldEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(this.desc);
		Signature translatedSignature = translator.translate(this.signature);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping,
				new FieldDefEntry(this.parent, translatedName, translatedDesc, translatedSignature, this.access, docs)
		);
	}

	@Override
	public FieldDefEntry withName(String name) {
		return new FieldDefEntry(this.parent, name, this.desc, this.signature, this.access, this.javadocs);
	}

	@Override
	public FieldDefEntry withParent(ClassEntry owner) {
		return new FieldDefEntry(owner, this.name, this.desc, this.signature, this.access, this.javadocs);
	}
}

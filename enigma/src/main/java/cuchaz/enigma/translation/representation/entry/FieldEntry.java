package cuchaz.enigma.translation.representation.entry;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.TypeDescriptor;

public class FieldEntry extends ParentedEntry<ClassEntry> implements Comparable<FieldEntry> {
	protected final TypeDescriptor desc;

	public FieldEntry(ClassEntry parent, String name, TypeDescriptor desc) {
		this(parent, name, desc, null);
	}

	public FieldEntry(ClassEntry parent, String name, TypeDescriptor desc, String javadocs) {
		super(parent, name, javadocs);

		Preconditions.checkNotNull(parent, "Owner cannot be null");
		Preconditions.checkNotNull(desc, "Field descriptor cannot be null");

		this.desc = desc;
	}

	public static FieldEntry parse(String owner, String name, String desc) {
		return new FieldEntry(new ClassEntry(owner), name, new TypeDescriptor(desc), null);
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	public TypeDescriptor getDesc() {
		return this.desc;
	}

	@Override
	public FieldEntry withName(String name) {
		return new FieldEntry(this.parent, name, this.desc, null);
	}

	@Override
	public FieldEntry withParent(ClassEntry parent) {
		return new FieldEntry(parent, this.name, this.desc, null);
	}

	@Override
	protected TranslateResult<FieldEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new FieldEntry(this.parent, translatedName, translator.translate(this.desc), docs)
		);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.name, this.desc);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldEntry fieldEntry && this.equals(fieldEntry);
	}

	public boolean equals(FieldEntry other) {
		return this.parent.equals(other.parent) && this.name.equals(other.name) && this.desc.equals(other.desc);
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return entry instanceof FieldEntry field && this.getParent().equals(field.getParent());
	}

	@Override
	public boolean canShadow(Entry<?> entry) {
		return entry instanceof FieldEntry;
	}

	@Override
	public String toString() {
		return this.getFullName() + ":" + this.desc;
	}

	@Override
	public int compareTo(FieldEntry entry) {
		return (this.name + this.desc.toString()).compareTo(entry.name + entry.desc.toString());
	}
}

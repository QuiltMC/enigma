package cuchaz.enigma.translation.representation.entry;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.MethodDescriptor;

public class MethodEntry extends ParentedEntry<ClassEntry> implements Comparable<MethodEntry> {
	protected final MethodDescriptor descriptor;

	public MethodEntry(ClassEntry parent, String name, MethodDescriptor descriptor) {
		this(parent, name, descriptor, null);
	}

	public MethodEntry(ClassEntry parent, String name, MethodDescriptor descriptor, String javadocs) {
		super(parent, name, javadocs);

		Preconditions.checkNotNull(parent, "Parent cannot be null");
		Preconditions.checkNotNull(descriptor, "Method descriptor cannot be null");

		this.descriptor = descriptor;
	}

	public static MethodEntry parse(String owner, String name, String desc) {
		return new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc), null);
	}

	@Override
	public Class<ClassEntry> getParentType() {
		return ClassEntry.class;
	}

	public MethodDescriptor getDesc() {
		return this.descriptor;
	}

	public boolean isConstructor() {
		return this.name.equals("<init>") || this.name.equals("<clinit>");
	}

	@Override
	protected TranslateResult<? extends MethodEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new MethodEntry(this.parent, translatedName, translator.translate(this.descriptor), docs)
		);
	}

	@Override
	public MethodEntry withName(String name) {
		return new MethodEntry(this.parent, name, this.descriptor, this.javadocs);
	}

	@Override
	public MethodEntry withParent(ClassEntry parent) {
		return new MethodEntry(new ClassEntry(parent.getFullName()), this.name, this.descriptor, this.javadocs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.name, this.descriptor);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodEntry methodEntry && this.equals(methodEntry);
	}

	public boolean equals(MethodEntry other) {
		return this.parent.equals(other.getParent()) && this.name.equals(other.getName()) && this.descriptor.equals(other.getDesc());
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		if (entry instanceof MethodEntry methodEntry) {
			return methodEntry.parent.equals(this.parent) && methodEntry.descriptor.canConflictWith(this.descriptor);
		}
		return false;
	}

	@Override
	public boolean canShadow(Entry<?> entry) {
		return entry instanceof MethodEntry;
	}

	@Override
	public String toString() {
		return this.getFullName() + this.descriptor;
	}

	@Override
	public int compareTo(MethodEntry entry) {
		return (this.name + this.descriptor.toString()).compareTo(entry.name + entry.descriptor.toString());
	}
}

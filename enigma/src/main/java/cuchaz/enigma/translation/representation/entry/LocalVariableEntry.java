package cuchaz.enigma.translation.representation.entry;

import com.google.common.base.Preconditions;
import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;

import java.util.Objects;
import javax.annotation.Nonnull;

public class LocalVariableEntry extends ParentedEntry<MethodEntry> implements Comparable<LocalVariableEntry> {
	protected final int index;
	protected final boolean parameter;

	public LocalVariableEntry(MethodEntry parent, int index, String name, boolean parameter, String javadoc) {
		super(parent, name, javadoc);

		Preconditions.checkNotNull(parent, "Variable owner cannot be null");
		Preconditions.checkArgument(index >= 0, "Index must be positive");

		this.index = index;
		this.parameter = parameter;
	}

	@Override
	public Class<MethodEntry> getParentType() {
		return MethodEntry.class;
	}

	public boolean isArgument() {
		return this.parameter;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	protected TranslateResult<LocalVariableEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String javadoc = mapping.javadoc();
		return TranslateResult.of(
				mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new LocalVariableEntry(this.parent, this.index, translatedName, this.parameter, javadoc)
		);
	}

	@Override
	public LocalVariableEntry withName(String name) {
		return new LocalVariableEntry(this.parent, this.index, name, this.parameter, this.javadocs);
	}

	@Override
	public LocalVariableEntry withParent(MethodEntry parent) {
		return new LocalVariableEntry(parent, this.index, this.name, this.parameter, this.javadocs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.index);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LocalVariableEntry localVariableEntry && this.equals(localVariableEntry);
	}

	public boolean equals(LocalVariableEntry other) {
		return this.parent.equals(other.parent) && this.index == other.index;
	}

	@Override
	public boolean canConflictWith(Entry<?> entry) {
		return entry instanceof LocalVariableEntry localVariableEntry && localVariableEntry.parent.equals(this.parent);
	}

	@Override
	public boolean canShadow(Entry<?> entry) {
		return false;
	}

	@Override
	public String toString() {
		return this.parent + "(" + this.index + ":" + this.name + ")";
	}

	@Override
	public int compareTo(LocalVariableEntry entry) {
		return Integer.compare(this.index, entry.index);
	}
}

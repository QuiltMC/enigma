package cuchaz.enigma.translation.mapping;

import javax.annotation.Nullable;

public record EntryMapping(
		@Nullable String targetName,
		@Nullable String javadoc
) {
	public static final EntryMapping DEFAULT = new EntryMapping(null, null);

	public EntryMapping(@Nullable String targetName) {
		this(targetName, null);
	}

	public EntryMapping withName(String newName) {
		return new EntryMapping(newName, this.javadoc);
	}

	public EntryMapping withDocs(String newDocs) {
		return new EntryMapping(this.targetName, newDocs);
	}
}

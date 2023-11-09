package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.source.RenamableTokenType;

import javax.annotation.Nullable;

public record EntryMapping(
		@Nullable String targetName,
		@Nullable String javadoc,
		RenamableTokenType tokenType,
		@Nullable String sourcePluginId
) {
	public static final EntryMapping DEFAULT = new EntryMapping(null, null, RenamableTokenType.OBFUSCATED, null);

	public EntryMapping(@Nullable String targetName) {
		this(targetName, null, RenamableTokenType.DEOBFUSCATED, null);
	}

	public EntryMapping withName(String newName) {
		return new EntryMapping(newName, this.javadoc, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withDocs(String newDocs) {
		return new EntryMapping(this.targetName, newDocs, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withTokenType(RenamableTokenType newTokenType) {
		return new EntryMapping(this.targetName, this.javadoc, newTokenType, this.sourcePluginId);
	}

	public EntryMapping withSourcePluginId(String newPluginId) {
		return new EntryMapping(this.targetName, this.javadoc, this.tokenType, newPluginId);
	}
}

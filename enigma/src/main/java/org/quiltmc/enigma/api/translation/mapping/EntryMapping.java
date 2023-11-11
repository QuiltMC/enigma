package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.source.TokenType;

import javax.annotation.Nullable;

public record EntryMapping(
		@Nullable String targetName,
		@Nullable String javadoc,
		TokenType tokenType,
		@Nullable String sourcePluginId
) {
	public static final EntryMapping DEFAULT = new EntryMapping(null, null, TokenType.OBFUSCATED, null);

	public EntryMapping(@Nullable String targetName) {
		this(targetName, null, targetName == null ? TokenType.OBFUSCATED : TokenType.DEOBFUSCATED, null);
	}

	public EntryMapping withName(String newName) {
		return new EntryMapping(newName, this.javadoc, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withDocs(String newDocs) {
		return new EntryMapping(this.targetName, newDocs, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withTokenType(TokenType newTokenType) {
		return new EntryMapping(this.targetName, this.javadoc, newTokenType, this.sourcePluginId);
	}

	public EntryMapping withSourcePluginId(String newPluginId) {
		return new EntryMapping(this.targetName, this.javadoc, this.tokenType, newPluginId);
	}

	public EntryMapping copy() {
		return new EntryMapping(this.targetName, this.javadoc, this.tokenType, this.sourcePluginId);
	}

	public static EntryMapping merge(EntryMapping leftMapping, EntryMapping rightMapping) {
		EntryMapping merged = leftMapping.copy();
		if (leftMapping.targetName == null && rightMapping.targetName != null) {
			merged = merged.withName(rightMapping.targetName).withTokenType(rightMapping.tokenType).withSourcePluginId(rightMapping.sourcePluginId);
		}

		if (leftMapping.javadoc == null && rightMapping.javadoc != null) {
			merged = merged.withDocs(rightMapping.javadoc);
		}

		return merged;
	}
}

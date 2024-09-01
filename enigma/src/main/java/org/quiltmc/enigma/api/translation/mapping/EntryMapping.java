package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.Enigma;
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
		this(trimWhitespace(targetName), null, targetName == null ? TokenType.OBFUSCATED : TokenType.DEOBFUSCATED, null);
	}

	public EntryMapping(@Nullable String targetName, @Nullable String javadoc) {
		this(trimWhitespace(targetName), javadoc, targetName == null ? TokenType.OBFUSCATED : TokenType.DEOBFUSCATED, null);
	}

	public EntryMapping {
		Enigma.validatePluginId(sourcePluginId);

		if (tokenType == null) {
			throw new RuntimeException("cannot create a mapping without a token type!");
		} else if (tokenType == TokenType.OBFUSCATED && targetName != null) {
			throw new RuntimeException("cannot create a named mapping with an obfuscated token type!");
		} else if (targetName == null && tokenType != TokenType.OBFUSCATED) {
			throw new RuntimeException("cannot create a non-obfuscated mapping with no name!");
		} else if (!tokenType.isProposed() && sourcePluginId != null) {
			throw new RuntimeException("cannot create a non-proposed mapping with a source plugin ID!");
		} else if (tokenType.isProposed() && sourcePluginId == null) {
			throw new RuntimeException("cannot create a proposed mapping with no source plugin ID!");
		} else if (tokenType.isProposed() && targetName == null) {
			throw new RuntimeException("cannot create a proposed mapping with no name!");
		}
	}

	public EntryMapping withName(@Nullable String newName) {
		return new EntryMapping(trimWhitespace(newName), this.javadoc, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withName(@Nullable String newName, TokenType tokenType) {
		return new EntryMapping(trimWhitespace(newName), this.javadoc, tokenType, this.sourcePluginId);
	}

	public EntryMapping withName(@Nullable String newName, TokenType tokenType, @Nullable String sourcePluginId) {
		return new EntryMapping(trimWhitespace(newName), this.javadoc, tokenType, sourcePluginId);
	}

	public EntryMapping withJavadoc(@Nullable String newDocs) {
		return new EntryMapping(this.targetName, newDocs, this.tokenType, this.sourcePluginId);
	}

	public EntryMapping withTokenType(TokenType newTokenType) {
		return new EntryMapping(this.targetName, this.javadoc, newTokenType, this.sourcePluginId);
	}

	public EntryMapping withSourcePluginId(@Nullable String newPluginId) {
		return new EntryMapping(this.targetName, this.javadoc, this.tokenType, newPluginId);
	}

	public EntryMapping copy() {
		return new EntryMapping(this.targetName, this.javadoc, this.tokenType, this.sourcePluginId);
	}

	/**
	 * Merges two mappings, filling in empty values on the left with values from the right.
	 * If a {@link #targetName} is picked up from the right mapping, its {@link #sourcePluginId} and {@link #tokenType} will be picked up as well.
	 * @param leftMapping the left mapping
	 * @param rightMapping the right mapping
	 * @return a new mapping with the values merged
	 */
	public static EntryMapping merge(EntryMapping leftMapping, EntryMapping rightMapping) {
		EntryMapping merged = leftMapping.copy();
		if (leftMapping.targetName == null && rightMapping.targetName != null) {
			merged = merged.withName(rightMapping.targetName, rightMapping.tokenType, rightMapping.sourcePluginId);
		}

		if (leftMapping.javadoc == null && rightMapping.javadoc != null) {
			merged = merged.withJavadoc(rightMapping.javadoc);
		}

		return merged;
	}

	private static String trimWhitespace(@Nullable String string) {
		return string == null ? null : string.strip();
	}
}

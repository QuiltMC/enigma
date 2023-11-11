package org.quiltmc.enigma.api.source;

/**
 * A token type dictates different types of mapping that an entry can have.
 */
public enum TokenType {
	/**
	 * Tokens which have no mapping whatsoever.
	 */
	OBFUSCATED,
	/**
	 * Tokens whose names have been read from a file or manually written by a user.
	 */
	DEOBFUSCATED,
	/**
	 * Tokens that have automatically proposed names based on bytecode.
	 */
	JAR_PROPOSED,
	/**
	 * Tokens that have automatically proposed names based on other mappings.
	 */
	DYNAMIC_PROPOSED,
	DEBUG;

	/**
	 * Checks whether this token has a name proposed by a plugin.
	 * @return whether this token is proposed
	 */
	public boolean isProposed() {
		return this == DYNAMIC_PROPOSED || this == JAR_PROPOSED;
	}
}

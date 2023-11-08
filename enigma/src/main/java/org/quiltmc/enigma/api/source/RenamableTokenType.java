package org.quiltmc.enigma.api.source;

public enum RenamableTokenType {
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
	DEBUG
}

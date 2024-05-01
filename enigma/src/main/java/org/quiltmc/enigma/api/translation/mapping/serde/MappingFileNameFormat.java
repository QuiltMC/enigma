package org.quiltmc.enigma.api.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

/**
 * Defines a strategy for naming files in directory-based mapping formats.
 */
public enum MappingFileNameFormat {
	/**
	 * Names files based on the mappings' obfuscated names.
	 * Example: if a class is mapped from {@code a} to {@code GreatClass}, its file will be named {@code GreatClass}.
	 */
	@SerializedName("by_obf")
	BY_OBF,
	/**
	 * Names files based on the mappings' deobfuscated names.
	 * Example: if a class is mapped from {@code a} to {@code GreatClass}, its file will be named {@code a}.
	 */
	@SerializedName("by_deobf")
	BY_DEOBF
}

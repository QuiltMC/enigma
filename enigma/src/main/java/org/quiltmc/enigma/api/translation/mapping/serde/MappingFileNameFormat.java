package org.quiltmc.enigma.api.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

/**
 * Defines a strategy for naming files in directory-based mapping formats.
 */
public enum MappingFileNameFormat {
	/**
	 * Names files based on their obfuscated names.
	 */
	@SerializedName("by_obf")
	BY_OBF,
	/**
	 * Names files based on their deobfuscated names.
	 */
	@SerializedName("by_deobf")
	BY_DEOBF
}

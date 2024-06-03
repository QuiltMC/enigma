package org.quiltmc.enigma.api.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public record MappingSaveParameters(
		@SerializedName("file_name_format") MappingFileNameFormat fileNameFormat,
		@SerializedName("write_proposed_names") boolean writeProposedNames,
		@SerializedName("obfuscated_namespace") @Nullable String obfuscatedNamespace,
		@SerializedName("deobfuscated_namespace") @Nullable String deobfuscatedNamespace
) {
	/**
	 * Controls how individual files will be named in directory-based mapping formats.
	 */
	@Override
	public MappingFileNameFormat fileNameFormat() {
		return this.fileNameFormat;
	}

	/**
	 * Controls how proposed names will be saved.
	 * If set to {@code true}, proposed names will be treated exactly the same as any other mapping when saving.
	 * If set to {@code false}, proposed names will not be saved -- on save, proposed names will be filtered out, but their javadocs will still be written.
	 */
	@Override
	public boolean writeProposedNames() {
		return this.writeProposedNames;
	}

	/**
	 * The namespace of the mappings' obfuscated names. This will be saved in certain mapping formats.
	 * If null, the format will define a default namespace.
	 */
	@Override
	public String obfuscatedNamespace() {
		return this.obfuscatedNamespace;
	}

	/**
	 * The namespace of the mappings' deobfuscated names. This will be saved in certain mapping formats.
	 * If null, the format will define a default namespace.
	 */
	@Override
	public String deobfuscatedNamespace() {
		return this.deobfuscatedNamespace;
	}
}

package org.quiltmc.enigma.api.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

public record MappingSaveParameters(@SerializedName("file_name_format") MappingFileNameFormat fileNameFormat, @SerializedName("write_proposed_names") boolean writeProposedNames) {
	@Override
	public MappingFileNameFormat fileNameFormat() {
		return this.fileNameFormat;
	}

	/**
	 * Controls how proposed names will be saved.
	 * If set to {@code true}, proposed names will be treated exactly the same as any other mapping when saving.
	 * If set to {@code false}, proposed names will not be saved -- on save, proposed names will be filtered out, but their javadocs will still be saved.
	 */
	@Override
	public boolean writeProposedNames() {
		return this.writeProposedNames;
	}
}

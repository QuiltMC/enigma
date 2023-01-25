package cuchaz.enigma.translation.mapping.serde;

import com.google.gson.annotations.SerializedName;

public record MappingSaveParameters(@SerializedName("file_name_format") MappingFileNameFormat fileNameFormat) {
	@Override
	public MappingFileNameFormat fileNameFormat() {
		return this.fileNameFormat;
	}
}

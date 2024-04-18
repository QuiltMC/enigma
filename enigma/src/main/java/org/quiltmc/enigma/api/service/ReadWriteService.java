package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;

import javax.annotation.Nullable;

public interface ReadWriteService extends EnigmaService {
	EnigmaServiceType<ReadWriteService> TYPE = EnigmaServiceType.create("read_write");

	@Nullable
	MappingsReader createReader();

	@Nullable
	MappingsWriter createWriter(String fromNamespace, String toNamespace);

	FileType getFileType();

	static ReadWriteService create(MappingsReader reader, MappingsWriter writer, FileType fileType, String id) {
		return new ReadWriteService() {
			@Override
			public String getId() {
				return id;
			}

			@Override
			public MappingsReader createReader() {
				return reader;
			}

			@Override
			public MappingsWriter createWriter(String fromNamespace, String toNamespace) {
				return writer;
			}

			@Override
			public FileType getFileType() {
				return fileType;
			}
		};
	}
}

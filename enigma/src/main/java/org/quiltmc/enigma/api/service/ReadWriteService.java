package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public interface ReadWriteService extends EnigmaService, MappingsWriter, MappingsReader {
	EnigmaServiceType<ReadWriteService> TYPE = EnigmaServiceType.create("read_write");

	FileType getFileType();

	static ReadWriteService create(MappingsReader reader, MappingsWriter writer, FileType fileType, String id) {
		return new ReadWriteService() {
			@Override
			public void write(@Nullable String obfNamespace, @Nullable String deobfNamespace, EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
				writer.write(obfNamespace, deobfNamespace, mappings, delta, path, progress, saveParameters);
			}

			@Override
			public EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws MappingParseException, IOException {
				return reader.read(path, progress);
			}

			@Override
			public String getId() {
				return id;
			}

			@Override
			public FileType getFileType() {
				return fileType;
			}
		};
	}
}

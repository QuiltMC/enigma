package org.quiltmc.enigma.api.service;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A read/write service defines a reader and/or a writer for mappings.
 * <br>
 * The service is keyed by a {@link FileType file type}, which is specified by a file extension. There should be no more than one read/write service per file type.
 * <br>
 * Read/write services are active by default, and as such do not need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface ReadWriteService extends EnigmaService, MappingsWriter, MappingsReader {
	EnigmaServiceType<ReadWriteService> TYPE = EnigmaServiceType.create("read_write", true);

	/**
	 * A unique file type for this service to read/write.
	 * This can either represent a directory or a single file.
	 * @return the file type
	 */
	FileType getFileType();

	/**
	 * {@return whether this service supports reading mappings}
	 */
	boolean supportsReading();

	/**
	 * {@return whether this service supports writing mappings}
	 */
	boolean supportsWriting();

	static ReadWriteService create(@Nullable MappingsReader reader, @Nullable MappingsWriter writer, FileType fileType, String id) {
		return new ReadWriteService() {
			@Override
			public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
				if (writer == null) {
					throw new UnsupportedOperationException("This service does not support writing!");
				}

				writer.write(mappings, delta, path, progress, saveParameters);
			}

			@Override
			public EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws MappingParseException, IOException {
				if (reader == null) {
					throw new UnsupportedOperationException("This service does not support reading!");
				}

				return reader.read(path, progress);
			}

			@Override
			public boolean supportsReading() {
				return reader != null;
			}

			@Override
			public boolean supportsWriting() {
				return writer != null;
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

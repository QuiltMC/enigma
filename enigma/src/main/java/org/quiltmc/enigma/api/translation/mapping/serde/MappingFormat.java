package org.quiltmc.enigma.api.translation.mapping.serde;

import com.google.common.io.MoreFiles;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.proguard.ProguardMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.srg.SrgMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Reader;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Writer;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.impl.translation.FileType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE, FileType.ENIGMA_MAPPING),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY, FileType.ENIGMA_DIRECTORY),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP, FileType.ENIGMA_ZIP),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader(), FileType.TINY_V2),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null, FileType.SRG),
	PROGUARD(null, ProguardMappingsReader.INSTANCE, FileType.PROGUARD);

	private final MappingsWriter writer;
	private final MappingsReader reader;
	private final FileType fileType;

	MappingFormat(MappingsWriter writer, MappingsReader reader, FileType fileType) {
		this.writer = writer;
		this.reader = reader;
		this.fileType = fileType;
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, ProgressListener.createEmpty(), saveParameters);
	}

	public void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, progressListener, saveParameters);
	}

	public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) {
		if (this.writer == null) {
			throw new IllegalStateException(this.name() + " does not support writing");
		}

		this.writer.write(mappings, delta, path, progressListener, saveParameters);
	}

	/**
	 * Reads the provided path and returns it as a tree of mappings.
	 * @param path the path to read
	 * @return a tree of every read mapping
	 * @throws IOException when there's an I/O error reading the files
	 * @throws MappingParseException when there's an issue with the content of a mapping file
	 */
	public EntryTree<EntryMapping> read(Path path) throws IOException, MappingParseException {
		if (this.reader == null) {
			throw new IllegalStateException(this.name() + " does not support reading");
		}

		return this.reader.read(path, ProgressListener.createEmpty());
	}

	/**
	 * Reads the provided path and returns it as a tree of mappings.
	 * @param path the path to read
	 * @param progressListener a progress listener to be used for displaying the progress to the user
	 * @return a tree of every read mapping
	 * @throws IOException when there's an I/O error reading the files
	 * @throws MappingParseException when there's an issue with the content of a mapping file
	 */
	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener) throws IOException, MappingParseException {
		if (this.reader == null) {
			throw new IllegalStateException(this.name() + " does not support reading");
		}

		return this.reader.read(path, progressListener);
	}

	@Nullable
	public MappingsWriter getWriter() {
		return this.writer;
	}

	@Nullable
	public MappingsReader getReader() {
		return this.reader;
	}

	public FileType getFileType() {
		return this.fileType;
	}

	/**
	 * Determines the mapping format of the provided file. Checks all formats according to their {@link #getFileType()} file extensions.
	 * If the file is a directory, it will check the first file in the directory.
	 * Will return {@link #PROGUARD} if no format is found for single files, and {@link #ENIGMA_DIRECTORY} if no format is found for directories.
	 * @param file the file to analyse
	 * @return the mapping format of the file.
	 */
	public static MappingFormat parseFromFile(Path file) {
		if (Files.isDirectory(file)) {
			try {
				File firstFile = Arrays.stream(Objects.requireNonNull(file.toFile().listFiles())).findFirst().orElseThrow();

				for (MappingFormat format : values()) {
					if (!format.getFileType().isDirectory()) {
						continue;
					}

					String extension = MoreFiles.getFileExtension(firstFile.toPath()).toLowerCase();
					if (format.fileType.getExtensions().contains(extension)) {
						return format;
					}
				}

				return ENIGMA_DIRECTORY;
			} catch (Exception e) {
				return ENIGMA_DIRECTORY;
			}
		} else {
			String extension = MoreFiles.getFileExtension(file).toLowerCase();

			for (MappingFormat format : values()) {
				if (format.fileType.getExtensions().contains(extension)) {
					return format;
				}
			}
		}

		return PROGUARD;
	}
}

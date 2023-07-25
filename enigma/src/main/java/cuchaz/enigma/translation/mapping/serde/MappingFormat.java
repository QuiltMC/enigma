package cuchaz.enigma.translation.mapping.serde;

import com.google.common.io.MoreFiles;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.proguard.ProguardMappingsReader;
import cuchaz.enigma.translation.mapping.serde.recaf.RecafMappingsReader;
import cuchaz.enigma.translation.mapping.serde.recaf.RecafMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.srg.SrgMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tiny.TinyMappingsReader;
import cuchaz.enigma.translation.mapping.serde.tiny.TinyMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Reader;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public enum MappingFormat {
	ENIGMA_FILE(EnigmaMappingsWriter.FILE, EnigmaMappingsReader.FILE),
	ENIGMA_DIRECTORY(EnigmaMappingsWriter.DIRECTORY, EnigmaMappingsReader.DIRECTORY),
	ENIGMA_ZIP(EnigmaMappingsWriter.ZIP, EnigmaMappingsReader.ZIP),
	TINY_V2(new TinyV2Writer("intermediary", "named"), new TinyV2Reader()),
	TINY_FILE(TinyMappingsWriter.INSTANCE, TinyMappingsReader.INSTANCE),
	SRG_FILE(SrgMappingsWriter.INSTANCE, null),
	PROGUARD(null, ProguardMappingsReader.INSTANCE),
	RECAF(RecafMappingsWriter.INSTANCE, RecafMappingsReader.INSTANCE);

	private final MappingsWriter writer;
	private final MappingsReader reader;

	MappingFormat(MappingsWriter writer, MappingsReader reader) {
		this.writer = writer;
		this.reader = reader;
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

	public EntryTree<EntryMapping> read(Path path, ProgressListener progressListener, MappingSaveParameters saveParameters) throws IOException, MappingParseException {
		if (this.reader == null) {
			throw new IllegalStateException(this.name() + " does not support reading");
		}

		return this.reader.read(path, progressListener, saveParameters);
	}

	@Nullable
	public MappingsWriter getWriter() {
		return this.writer;
	}

	@Nullable
	public MappingsReader getReader() {
		return this.reader;
	}

	/**
	 * Determines the mapping format of the provided file. Checks all formats, and returns {@link #RECAF} if none match.
	 * @param file the file to analyse
	 * @apiNote While most assessments are based on file extension, tiny format is determined by the contents of the file.
	 * If it contains a tiny header ("tiny[tab]2[tab]0"), the file is considered {@link #TINY_V2}. Otherwise, it goes to {@link #TINY_FILE}.
	 * Any directory is considered to be the {@link #ENIGMA_DIRECTORY} format. Recaf and Proguard formats are not determined by file extension,
	 * but we only check for Proguard, Recaf being the fallback.
	 * @return the mapping format of the file.
	 */
	public static MappingFormat parseFromFile(Path file) {
		if (Files.isDirectory(file)) {
			return ENIGMA_DIRECTORY;
		} else {
			try {
				switch (MoreFiles.getFileExtension(file).toLowerCase()) {
					case "zip" -> {
						return ENIGMA_ZIP;
					}
					case "mapping" -> {
						return ENIGMA_FILE;
					}
					case "tiny" -> {
						// the first line of a tiny v2 file should be a header with tiny[tab]2[tab]0
						String contents = Files.readString(file);

						if (contents.contains("tiny\t2\t0")) {
							return TINY_V2;
						} else {
							return TINY_FILE;
						}
					}
					case "tsrg" -> {
						return SRG_FILE;
					}
					default -> {
						// check for proguard. Recaf is the default if we don't match proguard here
						String contents = Files.readString(file);
						String firstLine = contents.split("\n")[0];
						String[] splitFirstLine = firstLine.split(" ");

						if (splitFirstLine.length == 3 && splitFirstLine[1].equals("->") && splitFirstLine[2].endsWith(":")) {
							return PROGUARD;
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("failed to read file \"" + file + "\" to parse mapping format!", e);
			}
		}

		return RECAF;
	}
}

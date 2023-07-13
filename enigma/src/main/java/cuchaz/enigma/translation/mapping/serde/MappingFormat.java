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
import java.util.Scanner;

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
	 * @apiNote While most assessments are based on file extension, tiny format is determined by the first line of the file.
	 * If the first line has "v2" in its header, the file is considered {@link #TINY_V2}. Otherwise, it goes to {@link #TINY_FILE}.
	 * Any directory is considered to be the {@link #ENIGMA_DIRECTORY} format.
	 * @return the mapping format of the file.
	 */
	public static MappingFormat parseFromFile(Path file) {
		if (Files.isDirectory(file)) {
			return ENIGMA_DIRECTORY;
		} else {
			switch (MoreFiles.getFileExtension(file).toLowerCase()) {
				case "zip" -> {
					return ENIGMA_ZIP;
				}
				case "mapping" -> {
					return ENIGMA_FILE;
				}
				case "tiny" -> {
					// the first line of a tiny v2 file should contain "v2"
					try (Scanner scanner = new Scanner(file)) {
						if (scanner.next().contains("v2")) {
							return TINY_V2;
						} else {
							return TINY_FILE;
						}
					} catch (IOException e) {
						return TINY_V2;
					}
				}
				case "tsrg" -> {
					return SRG_FILE;
				}
				default -> {
					// check for proguard. Recaf is the default if we don't match proguard here
					try (Scanner scanner = new Scanner(file)) {
						String[] firstLine = scanner.next().split(" ");
						if (firstLine.length == 3 && firstLine[1].equals("->") && firstLine[2].endsWith(":")) {
							return PROGUARD;
						}
					} catch (IOException e) {
						return RECAF;
					}
				}
			}
		}

		return RECAF;
	}
}

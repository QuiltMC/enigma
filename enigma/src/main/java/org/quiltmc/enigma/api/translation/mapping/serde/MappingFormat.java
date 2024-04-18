package org.quiltmc.enigma.api.translation.mapping.serde;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public class MappingFormat {
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
}

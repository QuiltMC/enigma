package org.quiltmc.enigma.api.translation.mapping.serde;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.io.IOException;
import java.nio.file.Path;

public interface MappingsReader {
	EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws MappingParseException, IOException;

	default EntryTree<EntryMapping> read(Path path) throws MappingParseException, IOException {
		return this.read(path, ProgressListener.createEmpty());
	}

	/**
	 * Reads at least the mappings for the given class, but may read more depending on the implementation.
	 * @implSpec The default implementation calls {@link #read(Path, ProgressListener)}, which reads all mappings.
	 */
	default EntryTree<EntryMapping> readClass(Path path, ClassEntry classEntry, ProgressListener progress) throws MappingParseException, IOException {
		return this.read(path, progress);
	}
}

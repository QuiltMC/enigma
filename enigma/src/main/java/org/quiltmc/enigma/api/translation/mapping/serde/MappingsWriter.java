package org.quiltmc.enigma.api.translation.mapping.serde;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.translation.mapping.serde.MappingSaveParameters;

import java.nio.file.Path;

public interface MappingsWriter {
	void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters);

	default void write(EntryTree<EntryMapping> mappings, Path path, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, ProgressListener.none(), saveParameters);
	}

	default void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, progress, saveParameters);
	}
}

package org.quiltmc.enigma.api.translation.mapping.serde;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;

import java.nio.file.Path;

public interface MappingsWriter {
	void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters);

	default void write(EntryTree<EntryMapping> mappings, Path path, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, ProgressListener.none(), saveParameters);
	}

	default void write(EntryTree<EntryMapping> mappings, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
		this.write(mappings, MappingDelta.added(mappings), path, progress, saveParameters);
	}

	/**
	 * Filters the mappings according to the provided parameters, removing entries that should not be saved. Does not modify the original tree.
	 * @param mappings the mappings to filter
	 * @param saveParameters the save parameters to use
	 * @return a new tree with only mappings that should be written
	 */
	static EntryTree<EntryMapping> filterMappings(EntryTree<EntryMapping> mappings, MappingSaveParameters saveParameters) {
		if (!saveParameters.writeProposedNames()) {
			EntryTree<EntryMapping> newMappings = new HashEntryTree<>();

			for (EntryTreeNode<EntryMapping> node : mappings) {
				EntryMapping mapping = node.getValue();

				if (mapping != null) {
					if (mapping.tokenType().isProposed() && mapping.javadoc() != null) {
						newMappings.insert(node.getEntry(), mapping.withName(null, TokenType.OBFUSCATED, null));
					} else if (!mapping.tokenType().isProposed()) {
						newMappings.insert(node);
					}
				}
			}

			return newMappings;
		}

		return mappings;
	}
}

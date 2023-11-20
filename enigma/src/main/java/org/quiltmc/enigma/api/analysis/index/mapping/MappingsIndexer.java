package org.quiltmc.enigma.api.analysis.index.mapping;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

/**
 * An indexer to collect information on a tree of mappings.
 */
public interface MappingsIndexer {
	default void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
	}

	default void indexMethodMapping(EntryMapping mapping, MethodEntry entry) {
	}

	default void indexFieldMapping(EntryMapping mapping, FieldEntry entry) {
	}

	default void indexLocalVariableMapping(EntryMapping mapping, LocalVariableEntry entry) {
	}

	/**
	 * Runs post-processing on the completed index.
	 * @param index the finished index
	 */
	default void processIndex(MappingsIndex index) {
	}

	/**
	 * Re-indexes the entry, discarding any previously existing data associated with it.
	 * This should only be called when a previously indexed entry's mapping changes.
	 * @param newMapping the entry's new mapping
	 * @param entry the entry to re-index
	 */
	void reindexEntry(EntryMapping newMapping, Entry<?> entry);

	/**
	 * A translation key for the title of this indexer.
	 * @return the translation key
	 */
	String getTranslationKey();
}

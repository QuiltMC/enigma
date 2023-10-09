package org.quiltmc.enigma.api.analysis.index.mapping;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

public interface MappingsIndexer {
	default void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
	}

	default void indexMethodMapping(EntryMapping mapping, MethodEntry entry) {
	}

	default void indexFieldMapping(EntryMapping mapping, FieldEntry entry) {
	}

	default void indexLocalVariableMapping(EntryMapping mapping, LocalVariableEntry entry) {
	}

	default void processIndex(MappingsIndex index) {
	}

	String getTranslationKey();
}

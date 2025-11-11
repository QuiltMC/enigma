package org.quiltmc.enigma.api.source;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

public interface Decompiler {
	default Source getUndocumentedSource(String className) {
		return this.getSource(className, null);
	}

	Source getSource(String className, @Nullable EntryRemapper remapper);
}

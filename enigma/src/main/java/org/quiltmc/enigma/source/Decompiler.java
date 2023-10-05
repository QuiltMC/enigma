package org.quiltmc.enigma.source;

import org.quiltmc.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Decompiler {
	default Source getUndocumentedSource(String className) {
		return this.getUndocumentedSource(className, null);
	}

	Source getUndocumentedSource(String className, @Nullable EntryRemapper remapper);
}

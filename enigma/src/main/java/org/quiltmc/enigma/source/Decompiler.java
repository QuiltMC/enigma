package org.quiltmc.enigma.source;

import org.quiltmc.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Decompiler {
	default Source getUndocumentedSource(String className) {
		return this.getSource(className, null);
	}

	Source getSource(String className, @Nullable EntryRemapper remapper);
}

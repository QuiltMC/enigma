package org.quiltmc.enigma.source;

import org.quiltmc.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Decompiler {
	Source getSource(String className, @Nullable EntryRemapper remapper);
}

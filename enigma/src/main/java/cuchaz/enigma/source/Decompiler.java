package cuchaz.enigma.source;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Decompiler {
	Source getSource(String className, @Nullable EntryRemapper remapper);
}

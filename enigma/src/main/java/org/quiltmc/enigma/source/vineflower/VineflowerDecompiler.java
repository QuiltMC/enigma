package org.quiltmc.enigma.source.vineflower;

import org.quiltmc.enigma.classprovider.ClassProvider;
import org.quiltmc.enigma.source.Decompiler;
import org.quiltmc.enigma.source.Source;
import org.quiltmc.enigma.source.SourceSettings;
import org.quiltmc.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VineflowerDecompiler implements Decompiler {
	private final ClassProvider classProvider;
	private final SourceSettings sourceSettings;

	public VineflowerDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.classProvider = classProvider;
		this.sourceSettings = sourceSettings;
	}

	@Override
	public Source getUndocumentedSource(String className, @Nullable EntryRemapper remapper) {
		return new VineflowerSource(new EnigmaContextSource(this.classProvider, className), remapper, this.sourceSettings);
	}
}

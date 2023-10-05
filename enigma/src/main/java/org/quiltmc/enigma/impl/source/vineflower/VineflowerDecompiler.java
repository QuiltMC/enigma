package org.quiltmc.enigma.impl.source.vineflower;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VineflowerDecompiler implements Decompiler {
	private final ClassProvider classProvider;
	private final SourceSettings sourceSettings;

	public VineflowerDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.classProvider = classProvider;
		this.sourceSettings = sourceSettings;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		return new VineflowerSource(new EnigmaContextSource(this.classProvider, className), remapper, this.sourceSettings);
	}
}

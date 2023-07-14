package cuchaz.enigma.source.vineflower;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
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

package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public class QuiltflowerDecompiler implements Decompiler {
	private final ClassProvider classProvider;
	private final SourceSettings sourceSettings;

	public QuiltflowerDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.classProvider = classProvider;
		this.sourceSettings = sourceSettings;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		return new QuiltflowerSource(new EnigmaContextSource(this.classProvider, className), remapper, this.sourceSettings);
	}
}

package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public class QuiltflowerDecompiler implements Decompiler {
    private final ClassProvider classProvider;

    public QuiltflowerDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
        this.classProvider = classProvider;
    }

    @Override
    public Source getSource(String className, @Nullable EntryRemapper remapper) {
        return new QuiltflowerSource(classProvider.get(className), remapper);
    }
}

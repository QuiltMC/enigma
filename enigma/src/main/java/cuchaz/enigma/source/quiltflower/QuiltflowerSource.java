package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.Map;

public class QuiltflowerSource implements Source {
    private final IContextSource contextSource;
    private EntryRemapper remapper;

    private SourceIndex index;

    public QuiltflowerSource(IContextSource contextSource, EntryRemapper remapper) {
        this.contextSource = contextSource;
        this.remapper = remapper;
    }

    // TODO: Remove imports
    private static Map<String, Object> getOptions(IFabricJavadocProvider javadocProvider) {
        Map<String, Object> options = QuiltflowerPreferences.getEffectiveOptions();
        options.put(IFabricJavadocProvider.PROPERTY_NAME, javadocProvider);
        return options;
    }

    @Override
    public String asString() {
        checkDecompiled();
        return index.getSource();
    }

    @Override
    public Source withJavadocs(EntryRemapper remapper) {
        this.remapper = remapper;
        this.index = null;
        return this;
    }

    @Override
    public SourceIndex index() {
        checkDecompiled();
        return index;
    }

    private void checkDecompiled() {
        if (index != null) {
            return;
        }

        index = new SourceIndex();

        IResultSaver saver = new EnigmaResultSaver(index);
        Map<String, Object> options = getOptions(new EnigmaJavadocProvider(remapper));
        IFernflowerLogger logger = new EnigmaFernflowerLogger();
        BaseDecompiler decompiler = new BaseDecompiler(saver, options, logger);

        decompiler.addSource(contextSource);

        decompiler.decompileContext();
    }
}

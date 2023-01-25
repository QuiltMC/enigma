package cuchaz.enigma.source.quiltflower;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class QuiltflowerSource implements Source {
	private final IContextSource contextSource;
	private final IContextSource libraryContextSource;
	private final boolean hasLibrarySource;
	private EntryRemapper remapper;
	private final SourceSettings settings;

	private SourceIndex index;

	public QuiltflowerSource(EnigmaContextSource contextSource, EntryRemapper remapper, SourceSettings settings) {
		this(contextSource, contextSource.getExternalSource(), remapper, settings);
	}

	public QuiltflowerSource(IContextSource contextSource, @Nullable IContextSource libraryContextSource, EntryRemapper remapper, SourceSettings settings) {
		this.contextSource = contextSource;
		this.libraryContextSource = libraryContextSource;
		this.hasLibrarySource = libraryContextSource != null;
		this.remapper = remapper;
		this.settings = settings;
	}

	private static Map<String, Object> getOptions(IFabricJavadocProvider javadocProvider, SourceSettings settings) {
		Map<String, Object> options = QuiltflowerPreferences.getEffectiveOptions();
		options.put(IFabricJavadocProvider.PROPERTY_NAME, javadocProvider);

		if (settings.removeImports()) {
			options.put(IFernflowerPreferences.REMOVE_IMPORTS, "1");
		}

		return options;
	}

	@Override
	public String asString() {
		this.checkDecompiled();
		return this.index.getSource();
	}

	@Override
	public Source withJavadocs(EntryRemapper remapper) {
		this.remapper = remapper;
		this.index = null;
		return this;
	}

	@Override
	public SourceIndex index() {
		this.checkDecompiled();
		return this.index;
	}

	private void checkDecompiled() {
		if (this.index != null) {
			return;
		}

		this.index = new SourceIndex();

		IResultSaver saver = new EnigmaResultSaver(this.index);
		Map<String, Object> options = getOptions(new EnigmaJavadocProvider(this.remapper), this.settings);
		IFernflowerLogger logger = new EnigmaFernflowerLogger();
		BaseDecompiler decompiler = new BaseDecompiler(saver, options, logger);

		AtomicReference<EnigmaTextTokenCollector> tokenCollector = new AtomicReference<>();
		TextTokenVisitor.addVisitor(next -> {
			tokenCollector.set(new EnigmaTextTokenCollector(next));
			return tokenCollector.get();
		});
		decompiler.addSource(this.contextSource);
		if (this.hasLibrarySource) decompiler.addLibrary(this.libraryContextSource);

		decompiler.decompileContext();

		if (this.settings.removeImports()) {
			removePackageStatement(this.index, tokenCollector.get());
		} else {
			tokenCollector.get().addTokensToIndex(this.index, token -> token);
		}
	}

	private static void removePackageStatement(SourceIndex index, EnigmaTextTokenCollector tokenCollector) {
		if (tokenCollector == null) {
			throw new IllegalStateException("No token collector");
		}

		String source = index.getSource();
		int start = source.indexOf("package");
		if (start < 0) {
			tokenCollector.addTokensToIndex(index, token -> token);
			return;
		}

		int end = index.getPosition(index.getLineNumber(start) + 1, 1);
		int offset = -(end - start) - 1;

		String newSource = source.substring(0, start) + source.substring(end + 1);
		index.setSource(newSource);
		tokenCollector.addTokensToIndex(index, token -> {
			if (token.start > end) {
				return token.move(offset);
			} else if (token.end <= start) {
				return token;
			} else {
				return null;
			}
		});
	}
}

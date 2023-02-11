package cuchaz.enigma.source.cfr;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CfrSource implements Source {
	private final String className;
	private final SourceSettings settings;
	private final Options options;
	private final ClassFileSource2 classFileSource;
	private final EntryRemapper mapper;

	private SourceIndex index;

	public CfrSource(String className, SourceSettings settings, Options options, ClassFileSource2 classFileSource, @Nullable EntryRemapper mapper) {
		this.className = className;
		this.settings = settings;
		this.options = options;
		this.classFileSource = classFileSource;
		this.mapper = mapper;
	}

	@Override
	public Source withJavadocs(EntryRemapper mapper) {
		return new CfrSource(this.className, this.settings, this.options, this.classFileSource, mapper);
	}

	@Override
	public SourceIndex index() {
		this.ensureDecompiled();
		return this.index;
	}

	@Override
	public String asString() {
		this.ensureDecompiled();
		return this.index.getSource();
	}

	private void ensureDecompiled() {
		if (this.index != null) {
			return;
		}

		DCCommonState commonState = new DCCommonState(this.options, this.classFileSource);
		ObfuscationMapping mapping = MappingFactory.get(this.options, commonState);
		DCCommonState state = new DCCommonState(commonState, mapping);
		ClassFile tree = state.getClassFileMaybePath(this.className);

		state.configureWith(tree);

		// To make sure we're analysing the cached version
		try {
			tree = state.getClassFile(tree.getClassType());
		} catch (CannotLoadClassException ignored) {
		}

		if (this.options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
			tree.loadInnerClasses(state);
		}

		if (this.options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)) {
			MemberNameResolver.resolveNames(state, ListFactory.newList(state.getClassCache().getLoadedTypes()));
		}

		TypeUsageCollectingDumper typeUsageCollector = new TypeUsageCollectingDumper(this.options, tree);
		tree.analyseTop(state, typeUsageCollector);

		EnigmaDumper dumper = new EnigmaDumper(new StringBuilder(), this.settings, typeUsageCollector.getRealTypeUsageInformation(), this.options, this.mapper);
		tree.dump(state.getObfuscationMapping().wrap(dumper));
		this.index = dumper.getIndex();
	}
}

package org.quiltmc.enigma.impl;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EnclosingMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.ObfuscationFixClassProvider;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.ObfuscationTestService;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeUtil;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.impl.bytecode.translator.TranslationClassVisitor;
import org.quiltmc.enigma.impl.plugin.EnumConstantIndexingService;
import org.quiltmc.enigma.impl.plugin.ParamSyntheticFieldIndexingService;
import org.quiltmc.enigma.impl.plugin.RecordIndexingService;
import org.quiltmc.enigma.impl.translation.mapping.MappingsChecker;
import org.quiltmc.enigma.util.I18n;
import org.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnigmaProjectImpl implements EnigmaProject {
	private final Enigma enigma;
	private final Path jarPath;
	private final ClassProvider classProvider;
	private final JarIndex jarIndex;
	private final JarIndex libIndex;
	private final JarIndex combinedIndex;
	private final byte[] jarChecksum;
	private final Map<MethodEntry, Boolean> libraryMethodOverrideCache = new HashMap<>();

	private EntryRemapper remapper;
	private MappingsIndex mappingsIndex;

	public EnigmaProjectImpl(Enigma enigma, Path jarPath, ClassProvider classProvider, JarIndex jarIndex, JarIndex libIndex, JarIndex combinedIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> proposedNames, byte[] jarChecksum) {
		Preconditions.checkArgument(jarChecksum.length == 20);
		this.enigma = enigma;
		this.jarPath = jarPath;
		this.classProvider = classProvider;
		this.jarIndex = jarIndex;
		this.libIndex = libIndex;
		this.combinedIndex = combinedIndex;
		this.jarChecksum = jarChecksum;

		this.mappingsIndex = mappingsIndex;
		this.remapper = EntryRemapper.mapped(this, proposedNames, new HashEntryTree<>());
	}

	@Override
	public void setMappings(@Nullable EntryTree<EntryMapping> mappings, ProgressListener progress) {
		// keep bytecode-based proposed names, to avoid unnecessary recalculation
		EntryTree<EntryMapping> jarProposedMappings = this.remapper != null ? this.remapper.getJarProposedMappings() : new HashEntryTree<>();

		this.mappingsIndex = MappingsIndex.empty();

		if (mappings != null) {
			EntryTree<EntryMapping> mergedTree = EntryTreeUtil.merge(jarProposedMappings, mappings);

			this.mappingsIndex.indexMappings(mergedTree, progress);
			this.remapper = EntryRemapper.mapped(this, jarProposedMappings, mappings);
		} else if (!jarProposedMappings.isEmpty()) {
			this.mappingsIndex.indexMappings(jarProposedMappings, progress);
			this.remapper = EntryRemapper.mapped(this, jarProposedMappings, new HashEntryTree<>());
		} else {
			this.remapper = EntryRemapper.empty(this);
		}

		// update dynamically proposed names
		this.remapper.insertDynamicallyProposedMappings(null, null, null);
	}

	@Override
	public Enigma getEnigma() {
		return this.enigma;
	}

	@Override
	public Path getJarPath() {
		return this.jarPath;
	}

	@Override
	public ClassProvider getClassProvider() {
		return this.classProvider;
	}

	@Override
	public JarIndex getJarIndex() {
		return this.jarIndex;
	}

	@Override
	public JarIndex getLibIndex() {
		return this.libIndex;
	}

	@Override
	public JarIndex getCombinedIndex() {
		return this.combinedIndex;
	}

	@Override
	public MappingsIndex getMappingsIndex() {
		return this.mappingsIndex;
	}

	@Override
	public byte[] getJarChecksum() {
		return Arrays.copyOf(this.jarChecksum, this.jarChecksum.length);
	}

	@Override
	public EntryRemapper getRemapper() {
		return this.remapper;
	}

	@Override
	public Collection<Entry<?>> dropMappings(ProgressListener progress) {
		DeltaTrackingTree<EntryMapping> mappings = this.remapper.getMappings();

		Collection<Entry<?>> dropped = this.dropMappings(mappings, progress);
		for (Entry<?> entry : dropped) {
			mappings.trackChange(entry);
		}

		return dropped;
	}

	private Collection<Entry<?>> dropMappings(EntryTree<EntryMapping> mappings, ProgressListener progress) {
		MappingsChecker.Dropper dropper = new MappingsChecker.Dropper();

		// drop mappings that don't match the jar
		MappingsChecker checker = new MappingsChecker(this, this.jarIndex, mappings);

		checker.collectBrokenMappings(progress, dropper);

		Map<Entry<?>, String> droppedBrokenMappings = dropper.getPendingDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedBrokenMappings.entrySet()) {
			Logger.warn("Couldn't find {} ({}) in jar. Mapping was dropped.", mapping.getKey(), mapping.getValue());
		}

		dropper.applyPendingDrops(mappings);
		checker.collectEmptyMappings(progress, dropper);

		Map<Entry<?>, String> droppedEmptyMappings = dropper.getPendingDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedEmptyMappings.entrySet()) {
			Logger.warn("{} ({}) was empty. Mapping was dropped.", mapping.getKey(), mapping.getValue());
		}

		dropper.applyPendingDrops(mappings);

		return dropper.getDroppedMappings().keySet();
	}

	@Override
	public boolean isNavigable(Entry<?> obfEntry) {
		if (obfEntry instanceof ClassEntry classEntry && this.isAnonymousOrLocal(classEntry)) {
			return false;
		}

		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(obfEntry);
	}

	public boolean isInternallyRenamable(Entry<?> obfEntry) {
		if (obfEntry instanceof MethodEntry obfMethodEntry) {
			// constructors are not renamable!
			if (obfMethodEntry.isConstructor()) {
				return false;
			}

			// HACKHACK: Object methods are not obfuscated identifiers
			String name = obfMethodEntry.getName();
			String sig = obfMethodEntry.getDesc().toString();

			// methods declared in object and record are not renamable
			// note: compareTo ignores parent, we want that
			if (this.libIndex.getChildrenByClass().get(new ClassEntry("java/lang/Object")).stream().anyMatch(c -> c instanceof MethodEntry m && m.compareTo(obfMethodEntry) == 0)
					|| this.libIndex.getChildrenByClass().get(new ClassEntry("java/lang/Record")).stream().anyMatch(c -> c instanceof MethodEntry m && m.compareTo(obfMethodEntry) == 0)) {
				return false;
			}

			ClassDefEntry parent = this.jarIndex.getIndex(EntryIndex.class).getDefinition(obfMethodEntry.getParent());
			if (parent != null && parent.isEnum()
					&& ((name.equals("values") && sig.equals("()[L" + parent.getFullName() + ";"))
					|| isEnumValueOfMethod(parent, obfMethodEntry))) {
				return false;
			}

			if (this.isLibraryMethodOverride(obfMethodEntry)) {
				return false;
			}
		} else if (obfEntry instanceof LocalVariableEntry localEntry) {
			if (!localEntry.isArgument()) {
				return false;
			}

			MethodEntry method = localEntry.getParent();
			ClassDefEntry parent = this.jarIndex.getIndex(EntryIndex.class).getDefinition(method.getParent());

			// if this is the valueOf method of an enum class, the argument shouldn't be able to be renamed.
			if (isEnumValueOfMethod(parent, method)) {
				return false;
			}
		} else if (obfEntry instanceof ClassEntry classEntry && this.isAnonymousOrLocal(classEntry)) {
			return false;
		}

		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(obfEntry);
	}

	private boolean isLibraryMethodOverride(MethodEntry methodEntry) {
		final Boolean cached = this.libraryMethodOverrideCache.get(methodEntry);
		if (cached != null) {
			return cached;
		} else {
			if (this.combinedIndex.getIndex(EntryIndex.class).hasMethod(methodEntry)) {
				final EntryResolver combinedResolver = this.combinedIndex.getEntryResolver();
				final Set<MethodEntry> equivalents = combinedResolver.resolveEquivalentMethods(methodEntry);
				final Set<MethodEntry> roots = equivalents.stream()
						.flatMap(equivalent -> combinedResolver.resolveEntry(equivalent, ResolutionStrategy.RESOLVE_ROOT).stream())
						.collect(Collectors.toSet());

				final Set<MethodEntry> equivalentsAndRoots = Stream
						.concat(equivalents.stream(), roots.stream())
						.collect(Collectors.toSet());

				final EntryIndex jarEntryIndex = this.jarIndex.getIndex(EntryIndex.class);
				final boolean anyNonJar = equivalentsAndRoots.stream().anyMatch(method -> !jarEntryIndex.hasMethod(method));

				equivalentsAndRoots.forEach(method -> this.libraryMethodOverrideCache.put(method, anyNonJar));

				return anyNonJar;
			} else {
				this.libraryMethodOverrideCache.put(methodEntry, false);

				return false;
			}
		}
	}

	@Override
	public boolean isRenamable(Entry<?> obfEntry) {
		if (this.isInternallyRenamable(obfEntry)) {
			if (obfEntry instanceof FieldEntry fieldEntry) {
				return !this.getEnumConstantIndexingService()
					.map(service -> service.isEnumConstant(fieldEntry))
					.orElse(false);
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	public Optional<EnumConstantIndexingService> getEnumConstantIndexingService() {
		return this.getEnigma()
			.getService(JarIndexerService.TYPE, EnumConstantIndexingService.ID)
			.map(service -> (EnumConstantIndexingService) service);
	}

	public Optional<RecordIndexingService> getRecordIndexingService() {
		return this.getEnigma()
			.getService(JarIndexerService.TYPE, RecordIndexingService.ID)
			.map(service -> (RecordIndexingService) service);
	}

	public Optional<ParamSyntheticFieldIndexingService> getParamSyntheticFieldIndexingService() {
		return this.getEnigma()
			.getService(JarIndexerService.TYPE, ParamSyntheticFieldIndexingService.ID)
			.map(service -> (ParamSyntheticFieldIndexingService) service);
	}

	private static boolean isEnumValueOfMethod(ClassDefEntry parent, MethodEntry method) {
		return parent != null && parent.isEnum() && method.getName().equals("valueOf") && method.getDesc().toString().equals("(Ljava/lang/String;)L" + parent.getFullName() + ";");
	}

	public boolean isInternallyRenamable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && this.isInternallyRenamable(obfReference.getNameableEntry(this));
	}

	@Override
	public boolean isRenamable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && this.isRenamable(obfReference.getNameableEntry(this));
	}

	@Override
	public boolean isObfuscated(Entry<?> entry) {
		List<ObfuscationTestService> obfuscationTestServices = this.getEnigma().getServices().get(ObfuscationTestService.TYPE);
		if (!obfuscationTestServices.isEmpty()) {
			for (ObfuscationTestService service : obfuscationTestServices) {
				if (service.testDeobfuscated(entry)) {
					return false;
				}
			}
		}

		EntryMapping mapping = this.remapper.getMapping(entry);
		return mapping.tokenType() == TokenType.OBFUSCATED;
	}

	@Override
	public boolean isSynthetic(Entry<?> entry) {
		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(entry) && this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(entry).isSynthetic();
	}

	@Override
	public boolean isAnonymousOrLocal(ClassEntry classEntry) {
		EnclosingMethodIndex enclosingMethodIndex = this.jarIndex.getIndex(EnclosingMethodIndex.class);
		// Only local and anonymous classes may have the EnclosingMethod attribute
		return enclosingMethodIndex.hasEnclosingMethod(classEntry);
	}

	@Override
	@SuppressWarnings("DataFlowIssue")
	public boolean validateParameterIndex(LocalVariableEntry parameter) {
		MethodEntry parent = parameter.getParent();
		EntryIndex index = this.jarIndex.getIndex(EntryIndex.class);

		if (index.hasMethod(parent)) {
			AtomicInteger maxLocals = new AtomicInteger(-1);
			ClassEntry parentClass = parent.getParent();

			// find max_locals for method, representing the number of parameters it receives (JVMS§4.7.3)
			// note: parent class cannot be null, warning suppressed
			ClassNode classNode = this.getClassProvider().get(parentClass.getFullName());
			if (classNode != null) {
				classNode.methods.stream()
						.filter(node -> node.name.equals(parent.getName()) && node.desc.equals(parent.getDesc().toString()))
						.findFirst().ifPresent(node -> {
							// occasionally it's possible to run into a method that has parameters, yet whose max locals is 0. java is stupid. we ignore those cases
							if (!(node.parameters != null && node.parameters.size() > node.maxLocals)) {
								maxLocals.set(node.maxLocals);
							}
						});
			}

			// if maxLocals is -1 it's not found for the method and should be ignored
			return index.validateParameterIndex(parameter) && (maxLocals.get() == -1 || parameter.getIndex() <= maxLocals.get() - 1);
		}

		return false;
	}

	@Override
	public JarExportImpl exportRemappedJar(ProgressListener progress) {
		Collection<ClassEntry> classEntries = this.jarIndex.getIndex(EntryIndex.class).getClasses();
		ClassProvider fixingClassProvider = new ObfuscationFixClassProvider(this.classProvider, this.jarIndex);
		Translator deobfuscator = this.remapper.getDeobfuscator();

		AtomicInteger count = new AtomicInteger();
		progress.init(classEntries.size(), I18n.translate("progress.classes.deobfuscating"));

		Map<String, ClassNode> compiled = classEntries.parallelStream()
				.map(entry -> {
					ClassEntry translatedEntry = deobfuscator.translate(entry);
					progress.step(count.getAndIncrement(), translatedEntry.toString());

					ClassNode node = fixingClassProvider.get(entry.getFullName());
					if (node != null) {
						ClassNode translatedNode = new ClassNode();
						node.accept(new TranslationClassVisitor(deobfuscator, Enigma.ASM_VERSION, translatedNode));
						return translatedNode;
					}

					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(n -> n.name, Functions.identity()));

		return new JarExportImpl(this.remapper, compiled);
	}

	public static final class JarExportImpl implements JarExport {
		private final EntryRemapper mapper;
		private final Map<String, ClassNode> compiled;

		JarExportImpl(EntryRemapper mapper, Map<String, ClassNode> compiled) {
			this.mapper = mapper;
			this.compiled = compiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(this.compiled.size(), I18n.translate("progress.jar.writing"));

			try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
				AtomicInteger count = new AtomicInteger();

				for (ClassNode node : this.compiled.values()) {
					progress.step(count.getAndIncrement(), node.name);

					String entryName = node.name.replace('.', '/') + ".class";

					ClassWriter writer = new ClassWriter(0);
					node.accept(writer);

					out.putNextEntry(new JarEntry(entryName));
					out.write(writer.toByteArray());
					out.closeEntry();
				}
			}
		}

		public SourceExport decompile(ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy) {
			List<ClassSourceImpl> decompiled = this.decompileStream(progress, decompilerService, errorStrategy).toList();
			return new SourceExportImpl(decompiled);
		}

		public Stream<ClassSourceImpl> decompileStream(ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy) {
			Collection<ClassNode> classes = this.compiled.values().stream()
					.filter(classNode -> classNode.name.indexOf('$') == -1)
					.toList();

			progress.init(classes.size(), I18n.translate("progress.classes.decompiling"));

			//create a common instance outside the loop as mappings shouldn't be changing while this is happening
			Decompiler decompiler = decompilerService.create(ClassProvider.fromMap(this.compiled), new SourceSettings(false, false));

			AtomicInteger count = new AtomicInteger();

			return classes.parallelStream()
					.map(translatedNode -> {
						progress.step(count.getAndIncrement(), translatedNode.name);

						String source = null;
						try {
							source = this.decompileClass(translatedNode, decompiler);
						} catch (Exception e) {
							switch (errorStrategy) {
								case PROPAGATE: throw e;
								case IGNORE: break;
								case TRACE_AS_SOURCE: {
									StringWriter writer = new StringWriter();
									e.printStackTrace(new PrintWriter(writer));
									source = writer.toString();
									break;
								}
							}
						}

						if (source == null) {
							return null;
						}

						return new ClassSourceImpl(translatedNode.name, source);
					})
					.filter(Objects::nonNull);
		}

		private String decompileClass(ClassNode translatedNode, Decompiler decompiler) {
			return decompiler.getSource(translatedNode.name, this.mapper).asString();
		}
	}

	public static final class SourceExportImpl implements SourceExport {
		public final Collection<ClassSourceImpl> decompiled;

		SourceExportImpl(Collection<ClassSourceImpl> decompiled) {
			this.decompiled = decompiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(this.decompiled.size(), I18n.translate("progress.sources.writing"));

			int count = 0;
			for (ClassSourceImpl source : this.decompiled) {
				progress.step(count++, source.name);

				Path sourcePath = source.resolvePath(path);
				source.writeTo(sourcePath);
			}
		}
	}

	public static class ClassSourceImpl implements ClassSource {
		public final String name;
		public final String source;

		ClassSourceImpl(String name, String source) {
			this.name = name;
			this.source = source;
		}

		public void writeTo(Path path) throws IOException {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write(this.source);
			}
		}

		public Path resolvePath(Path root) {
			return root.resolve(this.name.replace('.', '/') + ".java");
		}
	}
}

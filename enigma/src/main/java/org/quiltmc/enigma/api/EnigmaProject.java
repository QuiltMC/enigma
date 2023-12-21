package org.quiltmc.enigma.api;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EnclosingMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.service.ObfuscationTestService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeUtil;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.impl.bytecode.translator.TranslationClassVisitor;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.ObfuscationFixClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.DecompilerService;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.impl.translation.mapping.MappingsChecker;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnigmaProject {
	private static final List<Pair<String, String>> NON_RENAMABLE_METHODS = new ArrayList<>();

	static {
		NON_RENAMABLE_METHODS.add(new Pair<>("hashCode", "()I"));
		NON_RENAMABLE_METHODS.add(new Pair<>("clone", "()Ljava/lang/Object;"));
		NON_RENAMABLE_METHODS.add(new Pair<>("equals", "(Ljava/lang/Object;)Z"));
		NON_RENAMABLE_METHODS.add(new Pair<>("finalize", "()V"));
		NON_RENAMABLE_METHODS.add(new Pair<>("getClass", "()Ljava/lang/Class;"));
		NON_RENAMABLE_METHODS.add(new Pair<>("notify", "()V"));
		NON_RENAMABLE_METHODS.add(new Pair<>("notifyAll", "()V"));
		NON_RENAMABLE_METHODS.add(new Pair<>("toString", "()Ljava/lang/String;"));
		NON_RENAMABLE_METHODS.add(new Pair<>("wait", "()V"));
		NON_RENAMABLE_METHODS.add(new Pair<>("wait", "(J)V"));
		NON_RENAMABLE_METHODS.add(new Pair<>("wait", "(JI)V"));
	}

	private final Enigma enigma;

	private final Path jarPath;
	private final ClassProvider classProvider;
	private final JarIndex jarIndex;
	private MappingsIndex mappingsIndex;
	private final byte[] jarChecksum;

	private EntryRemapper remapper;

	public EnigmaProject(Enigma enigma, Path jarPath, ClassProvider classProvider, JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> proposedNames, byte[] jarChecksum) {
		Preconditions.checkArgument(jarChecksum.length == 20);
		this.enigma = enigma;
		this.jarPath = jarPath;
		this.classProvider = classProvider;
		this.jarIndex = jarIndex;
		this.jarChecksum = jarChecksum;

		this.mappingsIndex = mappingsIndex;
		this.remapper = EntryRemapper.mapped(jarIndex, this.mappingsIndex, proposedNames, new HashEntryTree<>(), this.enigma.getNameProposalServices());
	}

	/**
	 * Sets the current mappings of this project.
	 * Note that this triggers both an index of the mappings and dynamic name proposal, which may be expensive.
	 * @param mappings the new mappings
	 * @param progress a progress listener for indexing
	 */
	public void setMappings(@Nullable EntryTree<EntryMapping> mappings, ProgressListener progress) {
		// keep bytecode-based proposed names, to avoid unnecessary recalculation
		EntryTree<EntryMapping> jarProposedMappings = this.remapper != null ? this.remapper.getJarProposedMappings() : new HashEntryTree<>();

		this.mappingsIndex = MappingsIndex.empty();

		if (mappings != null) {
			EntryTree<EntryMapping> mergedTree = EntryTreeUtil.merge(jarProposedMappings, mappings);

			this.mappingsIndex.indexMappings(mergedTree, progress);
			this.remapper = EntryRemapper.mapped(this.jarIndex, this.mappingsIndex, jarProposedMappings, mappings, this.enigma.getNameProposalServices());
		} else if (!jarProposedMappings.isEmpty()) {
			this.mappingsIndex.indexMappings(jarProposedMappings, progress);
			this.remapper = EntryRemapper.mapped(this.jarIndex, this.mappingsIndex, jarProposedMappings, new HashEntryTree<>(), this.enigma.getNameProposalServices());
		} else {
			this.remapper = EntryRemapper.empty(this.jarIndex, this.enigma.getNameProposalServices());
		}

		// update dynamically proposed names
		this.remapper.insertDynamicallyProposedMappings(null, null, null);
	}

	public Enigma getEnigma() {
		return this.enigma;
	}

	public Path getJarPath() {
		return this.jarPath;
	}

	public ClassProvider getClassProvider() {
		return this.classProvider;
	}

	public JarIndex getJarIndex() {
		return this.jarIndex;
	}

	public MappingsIndex getMappingsIndex() {
		return this.mappingsIndex;
	}

	public byte[] getJarChecksum() {
		return this.jarChecksum;
	}

	public EntryRemapper getRemapper() {
		return this.remapper;
	}

	public void dropMappings(ProgressListener progress) {
		DeltaTrackingTree<EntryMapping> mappings = this.remapper.getMappings();

		Collection<Entry<?>> dropped = this.dropMappings(mappings, progress);
		for (Entry<?> entry : dropped) {
			mappings.trackChange(entry);
		}
	}

	private Collection<Entry<?>> dropMappings(EntryTree<EntryMapping> mappings, ProgressListener progress) {
		// drop mappings that don't match the jar
		MappingsChecker checker = new MappingsChecker(this.jarIndex, mappings);
		MappingsChecker.Dropped droppedBroken = checker.dropBrokenMappings(progress);

		Map<Entry<?>, String> droppedBrokenMappings = droppedBroken.getDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedBrokenMappings.entrySet()) {
			Logger.warn("Couldn't find {} ({}) in jar. Mapping was dropped.", mapping.getKey(), mapping.getValue());
		}

		MappingsChecker.Dropped droppedEmpty = checker.dropEmptyMappings(progress);

		Map<Entry<?>, String> droppedEmptyMappings = droppedEmpty.getDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedEmptyMappings.entrySet()) {
			Logger.warn("{} ({}) was empty. Mapping was dropped.", mapping.getKey(), mapping.getValue());
		}

		Collection<Entry<?>> droppedMappings = new HashSet<>();
		droppedMappings.addAll(droppedBrokenMappings.keySet());
		droppedMappings.addAll(droppedEmptyMappings.keySet());
		return droppedMappings;
	}

	public boolean isNavigable(Entry<?> obfEntry) {
		if (obfEntry instanceof ClassEntry classEntry && this.isAnonymousOrLocal(classEntry)) {
			return false;
		}

		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(obfEntry);
	}

	public boolean isRenamable(Entry<?> obfEntry) {
		if (obfEntry instanceof MethodEntry obfMethodEntry) {
			// constructors are not renamable!
			if (obfMethodEntry.isConstructor()) {
				return false;
			}

			// HACKHACK: Object methods are not obfuscated identifiers
			String name = obfMethodEntry.getName();
			String sig = obfMethodEntry.getDesc().toString();

			// todo change this to a check if the method is declared in java.lang.Object or java.lang.Record

			for (Pair<String, String> pair : NON_RENAMABLE_METHODS) {
				if (pair.a().equals(name) && pair.b().equals(sig)) {
					return false;
				}
			}

			ClassDefEntry parent = this.jarIndex.getIndex(EntryIndex.class).getDefinition(obfMethodEntry.getParent());
			if (parent != null && parent.isEnum()
					&& ((name.equals("values") && sig.equals("()[L" + parent.getFullName() + ";"))
					|| (name.equals("valueOf") && sig.equals("(Ljava/lang/String;)L" + parent.getFullName() + ";")))) {
				return false;
			}
		} else if (obfEntry instanceof LocalVariableEntry localEntry && !localEntry.isArgument()) {
			return false;
		} else if (obfEntry instanceof LocalVariableEntry localEntry && localEntry.isArgument()) {
			MethodEntry method = localEntry.getParent();
			ClassDefEntry parent = this.jarIndex.getIndex(EntryIndex.class).getDefinition(method.getParent());

			// if this is the valueOf method of an enum class, the argument shouldn't be able to be renamed.
			if (parent.isEnum() && method.getName().equals("valueOf") && method.getDesc().toString().equals("(Ljava/lang/String;)L" + parent.getFullName() + ";")) {
				return false;
			}
		} else if (obfEntry instanceof ClassEntry classEntry && this.isAnonymousOrLocal(classEntry)) {
			return false;
		}

		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(obfEntry);
	}

	public boolean isRenamable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && this.isRenamable(obfReference.getNameableEntry());
	}

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

	public boolean isSynthetic(Entry<?> entry) {
		return this.jarIndex.getIndex(EntryIndex.class).hasEntry(entry) && this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(entry).isSynthetic();
	}

	public boolean isAnonymousOrLocal(ClassEntry classEntry) {
		EnclosingMethodIndex enclosingMethodIndex = this.jarIndex.getIndex(EnclosingMethodIndex.class);
		// Only local and anonymous classes may have the EnclosingMethod attribute
		return enclosingMethodIndex.hasEnclosingMethod(classEntry);
	}

	public JarExport exportRemappedJar(ProgressListener progress) {
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

		return new JarExport(this.remapper, compiled);
	}

	public static final class JarExport {
		private final EntryRemapper mapper;
		private final Map<String, ClassNode> compiled;

		JarExport(EntryRemapper mapper, Map<String, ClassNode> compiled) {
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
			List<ClassSource> decompiled = this.decompileStream(progress, decompilerService, errorStrategy).toList();
			return new SourceExport(decompiled);
		}

		public Stream<ClassSource> decompileStream(ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy) {
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

						return new ClassSource(translatedNode.name, source);
					})
					.filter(Objects::nonNull);
		}

		private String decompileClass(ClassNode translatedNode, Decompiler decompiler) {
			return decompiler.getSource(translatedNode.name, this.mapper).asString();
		}
	}

	public static final class SourceExport {
		public final Collection<ClassSource> decompiled;

		SourceExport(Collection<ClassSource> decompiled) {
			this.decompiled = decompiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(this.decompiled.size(), I18n.translate("progress.sources.writing"));

			int count = 0;
			for (ClassSource source : this.decompiled) {
				progress.step(count++, source.name);

				Path sourcePath = source.resolvePath(path);
				source.writeTo(sourcePath);
			}
		}
	}

	public static class ClassSource {
		public final String name;
		public final String source;

		ClassSource(String name, String source) {
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

	public enum DecompileErrorStrategy {
		PROPAGATE,
		TRACE_AS_SOURCE,
		IGNORE
	}
}

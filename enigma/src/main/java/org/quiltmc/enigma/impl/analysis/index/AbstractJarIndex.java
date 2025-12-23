package org.quiltmc.enigma.impl.analysis.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndexer;
import org.quiltmc.enigma.api.analysis.index.jar.ReferenceIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.util.I18n;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractJarIndex implements JarIndex {
	private final Set<String> indexedClasses = new HashSet<>();
	private final ImmutableMap<Class<? extends JarIndexer>, JarIndexer> indexers;
	private final IndexEntryResolver entryResolver;

	private final Multimap<String, MethodDefEntry> methodImplementations = HashMultimap.create();
	private final ListMultimap<ClassEntry, ParentedEntry<?>> childrenByClass;

	private ProgressListener progress;

	/**
	 * Creates a new empty index with all provided indexers.
	 * Indexers will be run in the order they're passed to this constructor.
	 *
	 * @param entryIndex        the entry index
	 * @param inheritanceIndex  the inheritance index
	 * @param referenceIndex    the reference index
	 * @param bridgeMethodIndex the bridge method index
	 * @param otherIndexers	    other indexers to use
	 */
	public AbstractJarIndex(
			EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		this.indexers = Stream
				.concat(
					Stream.of(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex),
					Arrays.stream(otherIndexers)
				)
				.collect(ImmutableMap.toImmutableMap(
					JarIndexer::getType,
					Function.identity()
				));

		this.entryResolver = new IndexEntryResolver(this);
		this.childrenByClass = ArrayListMultimap.create();
	}

	/**
	 * Gets the index associated with the provided class.
	 * @param clazz the class of the index desired - for example, {@code PackageIndex.class}
	 * @return the index
	 */
	@SuppressWarnings("unchecked")
	public <T extends JarIndexer> T getIndex(Class<T> clazz) {
		JarIndexer index = this.indexers.get(clazz);
		if (index != null) {
			return (T) index;
		} else {
			throw new IllegalArgumentException("no indexer registered for class " + clazz);
		}
	}

	@Override
	public void indexJar(ProjectClassProvider classProvider, ProgressListener progress) {
		this.indexJar(this.getIndexableClassNames(classProvider), classProvider, progress);
	}

	/**
	 * Runs every configured indexer over the provided jar on classes matching the passed {@code classNameFilter}.
	 *
	 * @param classProvider   a class provider containing all classes in the jar and libraries
	 * @param progress        a progress listener to track index completion
	 * @param classNameFilter a predicate used to filter out class names
	 */
	public void indexJar(
			ProjectClassProvider classProvider, ProgressListener progress, Predicate<String> classNameFilter
	) {
		final Collection<String> classNames = this.getIndexableClassNames(classProvider).stream()
				.filter(classNameFilter)
				.collect(Collectors.toSet());
		this.indexJar(classNames, classProvider, progress);
	}

	/**
	 * Gets the names of classes this indexer should index.
	 *
	 * @param classProvider a class provider containing all classes in the jar
	 */
	public abstract Collection<String> getIndexableClassNames(ProjectClassProvider classProvider);

	/**
	 * Runs every configured indexer over the provided jar.
	 *
	 * @param classProvider a class provider containing all classes in the jar
	 * @param progress a progress listener to track index completion
	 */
	protected void indexJar(Collection<String> classNames, ClassProvider classProvider, ProgressListener progress) {
		// for use in processIndex
		this.progress = progress;

		this.indexedClasses.addAll(classNames);
		this.progress.init(4, I18n.translate("progress.jar.indexing"));

		this.progress.step(1, I18n.translate("progress.jar.indexing.entries"));

		for (String className : classNames) {
			Objects.requireNonNull(classProvider.get(className)).accept(new IndexClassVisitor(this, Enigma.ASM_VERSION));
		}

		this.progress.step(2, I18n.translate("progress.jar.indexing.references"));

		for (String className : classNames) {
			try {
				Objects.requireNonNull(classProvider.get(className)).accept(new IndexReferenceVisitor(this, this.getIndex(EntryIndex.class), this.getIndex(InheritanceIndex.class), Enigma.ASM_VERSION));
			} catch (Exception e) {
				throw new RuntimeException("Exception while indexing class: " + className, e);
			}
		}

		this.progress.step(3, I18n.translate("progress.jar.indexing.methods"));
		this.getIndex(BridgeMethodIndex.class).findBridgeMethods();

		this.processIndex(this);

		this.progress = null;
	}

	@Override
	public void processIndex(JarIndex index) {
		this.stepProcessingProgress("progress.jar.indexing.process.jar");

		this.indexers.forEach((key, indexer) -> {
			this.stepProcessingProgress(indexer.getTranslationKey());
			indexer.processIndex(index);
		});

		this.stepProcessingProgress("progress.jar.indexing.process.done");
	}

	private void stepProcessingProgress(String key) {
		if (this.progress != null) {
			this.progress.step(4, I18n.translateFormatted("progress.jar.indexing.process", I18n.translate(key)));
		}
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			if (classEntry.equals(interfaceEntry)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + classEntry);
			}
		}

		this.indexers.forEach((key, indexer) -> indexer.indexClass(classEntry));
		if (classEntry.isInnerClass() && !classEntry.getAccess().isSynthetic()) {
			this.childrenByClass.put(classEntry.getParent(), classEntry);
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		this.indexers.forEach((key, indexer) -> indexer.indexField(fieldEntry));
		if (!fieldEntry.getAccess().isSynthetic()) {
			this.childrenByClass.put(fieldEntry.getParent(), fieldEntry);
		}
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		this.indexers.forEach((key, indexer) -> indexer.indexMethod(methodEntry));
		if (!methodEntry.getAccess().isSynthetic() && !methodEntry.getName().equals("<clinit>")) {
			this.childrenByClass.put(methodEntry.getParent(), methodEntry);
		}

		if (!methodEntry.isConstructor()) {
			this.methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	@Override
	public void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
		this.indexers.forEach((key, indexer) -> indexer.indexClassReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		this.indexers.forEach((key, indexer) -> indexer.indexMethodReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		this.indexers.forEach((key, indexer) -> indexer.indexFieldReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		this.indexers.forEach((key, indexer) -> indexer.indexLambda(callerEntry, lambda, targetType));
	}

	@Override
	public void indexEnclosingMethod(ClassDefEntry classEntry, EnclosingMethodData enclosingMethodData) {
		this.indexers.forEach((key, indexer) -> indexer.indexEnclosingMethod(classEntry, enclosingMethodData));
	}

	@Override
	public EntryResolver getEntryResolver() {
		return this.entryResolver;
	}

	@Override
	public ListMultimap<ClassEntry, ParentedEntry<?>> getChildrenByClass() {
		return this.childrenByClass;
	}

	@Override
	public boolean isIndexed(String internalName) {
		return this.indexedClasses.contains(internalName);
	}
}


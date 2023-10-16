package org.quiltmc.enigma.api.analysis.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.impl.analysis.index.IndexClassVisitor;
import org.quiltmc.enigma.impl.analysis.index.IndexReferenceVisitor;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JarIndex implements JarIndexer {
	private final Set<String> indexedClasses = new HashSet<>();
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;
	private final PackageVisibilityIndex packageVisibilityIndex;
	private final EnclosingMethodIndex enclosingMethodIndex;
	private final EntryResolver entryResolver;

	private final Collection<JarIndexer> indexers;

	private final Multimap<String, MethodDefEntry> methodImplementations = HashMultimap.create();
	private final ListMultimap<ClassEntry, ParentedEntry<?>> childrenByClass;

	private ProgressListener progress;

	public JarIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex, BridgeMethodIndex bridgeMethodIndex, PackageVisibilityIndex packageVisibilityIndex, EnclosingMethodIndex enclosingMethodIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
		this.packageVisibilityIndex = packageVisibilityIndex;
		this.enclosingMethodIndex = enclosingMethodIndex;
		this.indexers = List.of(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex, enclosingMethodIndex);
		this.entryResolver = new IndexEntryResolver(this);
		this.childrenByClass = ArrayListMultimap.create();
	}

	public static JarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex);
		PackageVisibilityIndex packageVisibilityIndex = new PackageVisibilityIndex();
		EnclosingMethodIndex enclosingMethodIndex = new EnclosingMethodIndex();
		return new JarIndex(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex, enclosingMethodIndex);
	}

	public void indexJar(Set<String> classNames, ClassProvider classProvider, ProgressListener progress) {
		// for use in processIndex
		this.progress = progress;

		this.indexedClasses.addAll(classNames);
		this.progress.init(4, I18n.translate("progress.jar.indexing"));

		this.progress.step(1, I18n.translate("progress.jar.indexing.entries"));

		for (String className : classNames) {
			classProvider.get(className).accept(new IndexClassVisitor(this, Enigma.ASM_VERSION));
		}

		this.progress.step(2, I18n.translate("progress.jar.indexing.references"));

		for (String className : classNames) {
			try {
				classProvider.get(className).accept(new IndexReferenceVisitor(this, this.entryIndex, this.inheritanceIndex, Enigma.ASM_VERSION));
			} catch (Exception e) {
				throw new RuntimeException("Exception while indexing class: " + className, e);
			}
		}

		this.progress.step(3, I18n.translate("progress.jar.indexing.methods"));
		this.bridgeMethodIndex.findBridgeMethods();

		this.processIndex(this);

		this.progress = null;
	}

	@Override
	public void processIndex(JarIndex index) {
		this.stepProcessingProgress("progress.jar.indexing.process.jar");

		this.indexers.forEach(indexer -> {
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
		if (classEntry.isJre()) {
			return;
		}

		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			if (classEntry.equals(interfaceEntry)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + classEntry);
			}
		}

		this.indexers.forEach(indexer -> indexer.indexClass(classEntry));
		if (classEntry.isInnerClass() && !classEntry.getAccess().isSynthetic()) {
			this.childrenByClass.put(classEntry.getParent(), classEntry);
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		if (fieldEntry.getParent().isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexField(fieldEntry));
		if (!fieldEntry.getAccess().isSynthetic()) {
			this.childrenByClass.put(fieldEntry.getParent(), fieldEntry);
		}
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		if (methodEntry.getParent().isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexMethod(methodEntry));
		if (!methodEntry.getAccess().isSynthetic() && !methodEntry.getName().equals("<clinit>")) {
			this.childrenByClass.put(methodEntry.getParent(), methodEntry);
		}

		if (!methodEntry.isConstructor()) {
			this.methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexMethodReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexFieldReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexLambda(callerEntry, lambda, targetType));
	}

	@Override
	public void indexEnclosingMethod(ClassDefEntry classEntry, EnclosingMethodData enclosingMethodData) {
		if (classEntry.isJre()) {
			return;
		}

		this.indexers.forEach(indexer -> indexer.indexEnclosingMethod(classEntry, enclosingMethodData));
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.jar";
	}

	public EntryIndex getEntryIndex() {
		return this.entryIndex;
	}

	public InheritanceIndex getInheritanceIndex() {
		return this.inheritanceIndex;
	}

	public ReferenceIndex getReferenceIndex() {
		return this.referenceIndex;
	}

	public BridgeMethodIndex getBridgeMethodIndex() {
		return this.bridgeMethodIndex;
	}

	public PackageVisibilityIndex getPackageVisibilityIndex() {
		return this.packageVisibilityIndex;
	}

	public EnclosingMethodIndex getEnclosingMethodIndex() {
		return this.enclosingMethodIndex;
	}

	public EntryResolver getEntryResolver() {
		return this.entryResolver;
	}

	public ListMultimap<ClassEntry, ParentedEntry<?>> getChildrenByClass() {
		return this.childrenByClass;
	}

	public boolean isIndexed(String internalName) {
		return this.indexedClasses.contains(internalName);
	}
}
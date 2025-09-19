package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class CombinedJarIndex extends AbstractJarIndex {
	public CombinedJarIndex(JarIndexer... indexers) {
		super(indexers);
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static JarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
		ReferenceIndex referenceIndex = new ReferenceIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		return new CombinedJarIndex(entryIndex, inheritanceIndex, referenceIndex, new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex));
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.combined";
	}

	@Override
	public void indexJar(ProjectClassProvider classProvider, ProgressListener progress) {
		this.indexJar(classProvider.getClassNames(), classProvider, progress);
	}
}

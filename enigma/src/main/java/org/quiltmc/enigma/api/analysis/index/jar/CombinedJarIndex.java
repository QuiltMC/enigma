package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

import java.util.Collection;

public class CombinedJarIndex extends AbstractJarIndex {
	public CombinedJarIndex(
			EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, otherIndexers);
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static CombinedJarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
		ReferenceIndex referenceIndex = new ReferenceIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		return new CombinedJarIndex(
				entryIndex, inheritanceIndex, referenceIndex,
				new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex)
		);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.combined";
	}

	@Override
	public Collection<String> getIndexableClassNames(ProjectClassProvider classProvider) {
		return classProvider.getClassNames();
	}
}

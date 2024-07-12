package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

public class LibrariesJarIndex extends AbstractJarIndex {
	public LibrariesJarIndex(JarIndexer... indexers) {
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
		return new LibrariesJarIndex(entryIndex, inheritanceIndex, referenceIndex, new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex));
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.libraries";
	}
}

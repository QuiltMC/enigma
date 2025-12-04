package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

import java.util.Collection;

/**
 * An index of the library jars of an {@link EnigmaProject}.
 */
public class LibrariesJarIndex extends AbstractJarIndex {
	final EntryIndex entryIndex;
	final ReferenceIndex referenceIndex;
	final BridgeMethodIndex bridgeMethodIndex;

	public LibrariesJarIndex(
			EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, otherIndexers);
		this.entryIndex = entryIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static LibrariesJarIndex empty() {
		EntryIndex entryIndex = new EntryIndexImpl();
		ReferenceIndex referenceIndex = new ReferenceIndexImpl();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		return new LibrariesJarIndex(
				entryIndex, inheritanceIndex, referenceIndex,
				new BridgeMethodIndexImpl(entryIndex, inheritanceIndex, referenceIndex)
		);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.libraries";
	}

	@Override
	public Collection<String> getIndexableClassNames(ProjectClassProvider classProvider) {
		return classProvider.getLibraryClassNames();
	}
}

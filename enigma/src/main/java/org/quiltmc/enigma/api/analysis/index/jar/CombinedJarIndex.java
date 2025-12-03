package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

import java.util.Collection;

/**
 * An index of the main jar <em>and</em> library jars of an {@link EnigmaProject}.
 */
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
	public static CombinedJarIndex empty(MainJarIndex mainIndex, LibrariesJarIndex libIndex) {
		EntryIndex entryIndex = new CombinedEntryIndex(mainIndex.entryIndex, libIndex.entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		LambdaIndex lambdaIndex = new LambdaIndex();
		return new CombinedJarIndex(
				entryIndex, inheritanceIndex, referenceIndex,
				new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex),
				// required by MappingValidator
				lambdaIndex
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

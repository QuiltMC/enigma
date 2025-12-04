package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;

import java.util.Collection;

/**
 * An index of the main jar of an {@link EnigmaProject}.
 */
public class MainJarIndex extends IndependentJarIndex {
	public MainJarIndex(
			EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, otherIndexers);
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static MainJarIndex empty() {
		EntryIndex entryIndex = new EntryIndexImpl();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndexImpl();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndexImpl(entryIndex, inheritanceIndex, referenceIndex);
		PackageVisibilityIndex packageVisibilityIndex = new PackageVisibilityIndex();
		EnclosingMethodIndex enclosingMethodIndex = new EnclosingMethodIndex();
		LambdaIndex lambdaIndex = new LambdaIndex();
		return new MainJarIndex(
				entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex,
				packageVisibilityIndex, enclosingMethodIndex, lambdaIndex
		);
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.jar";
	}

	@Override
	public Collection<String> getIndexableClassNames(ProjectClassProvider classProvider) {
		return classProvider.getMainClassNames();
	}
}

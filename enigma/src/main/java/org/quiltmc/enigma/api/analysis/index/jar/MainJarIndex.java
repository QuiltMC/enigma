package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

import java.util.Collection;

/**
 * An index of the main jar of an {@link EnigmaProject}.
 */
public class MainJarIndex extends AbstractJarIndex {
	final EntryIndexImpl entryIndex;

	public MainJarIndex(
			EntryIndexImpl entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, otherIndexers);
		this.entryIndex = entryIndex;
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static MainJarIndex empty() {
		EntryIndexImpl entryIndex = new EntryIndexImpl();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex);
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

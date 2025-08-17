package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

public class MainJarIndex extends AbstractJarIndex {
	public MainJarIndex(JarIndexer... indexers) {
		super(indexers);
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static JarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
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
	public void indexJar(ProjectClassProvider classProvider, ProgressListener progress) {
		this.indexJar(classProvider.getMainClassNames(), classProvider, progress);
	}
}

package org.quiltmc.enigma.api.analysis.index.jar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Map;

public sealed interface BridgeMethodIndex extends JarIndexer
		permits CombinedBridgeMethodIndex, IndependentBridgeMethodIndex {
	void findBridgeMethods();

	boolean isBridgeMethod(MethodEntry entry);

	boolean isSpecializedMethod(MethodEntry entry);

	@Nullable
	MethodEntry getBridgeFromSpecialized(MethodEntry specialized);

	MethodEntry getSpecializedFromBridge(MethodEntry bridge);

	/**
	 * Includes "renamed specialized -> bridge" entries.
	 */
	Map<MethodEntry, MethodEntry> getSpecializedToBridge();

	/**
	 * Only "bridge -> original name" entries.
	 */
	Map<MethodEntry, MethodEntry> getBridgeToSpecialized();

	@Override
	default Class<? extends JarIndexer> getType() {
		return BridgeMethodIndex.class;
	}

	@Override
	default String getTranslationKey() {
		return "progress.jar.indexing.process.bridge_methods";
	}
}

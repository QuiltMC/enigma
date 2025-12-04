package org.quiltmc.enigma.api.analysis.index.jar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.UnmodifiableCombinedMap;

import java.util.Map;

class CombinedBridgeMethodIndex implements BridgeMethodIndex {
	private final BridgeMethodIndex mainIndex;
	private final BridgeMethodIndex libIndex;
	private final Map<MethodEntry, MethodEntry> specializedToBridge;
	private final Map<MethodEntry, MethodEntry> bridgeToSpecialized;

	CombinedBridgeMethodIndex(BridgeMethodIndex mainIndex, BridgeMethodIndex libIndex) {
		this.mainIndex = mainIndex;
		this.libIndex = libIndex;

		this.specializedToBridge = new UnmodifiableCombinedMap<>(
			this.mainIndex.getSpecializedToBridge(),
			this.libIndex.getSpecializedToBridge()
		);

		this.bridgeToSpecialized = new UnmodifiableCombinedMap<>(
			this.mainIndex.getBridgeToSpecialized(),
			this.libIndex.getBridgeToSpecialized()
		);
	}

	@Override
	public void findBridgeMethods() {
		// this is done by the main and lib delegates
	}

	@Override
	public boolean isBridgeMethod(MethodEntry entry) {
		return this.mainIndex.isBridgeMethod(entry) || this.libIndex.isBridgeMethod(entry);
	}

	@Override
	public boolean isSpecializedMethod(MethodEntry entry) {
		return this.mainIndex.isSpecializedMethod(entry) || this.libIndex.isSpecializedMethod(entry);
	}

	@Override
	public @Nullable MethodEntry getBridgeFromSpecialized(MethodEntry specialized) {
		final MethodEntry mainBridge = this.mainIndex.getBridgeFromSpecialized(specialized);
		return mainBridge == null ? this.libIndex.getBridgeFromSpecialized(specialized) : mainBridge;
	}

	@Override
	public MethodEntry getSpecializedFromBridge(MethodEntry bridge) {
		final MethodEntry mainSpecialized = this.mainIndex.getSpecializedFromBridge(bridge);
		return mainSpecialized == null ? this.libIndex.getSpecializedFromBridge(bridge) : mainSpecialized;
	}

	@Override
	public Map<MethodEntry, MethodEntry> getSpecializedToBridge() {
		return this.specializedToBridge;
	}

	@Override
	public Map<MethodEntry, MethodEntry> getBridgeToSpecialized() {
		return this.bridgeToSpecialized;
	}
}

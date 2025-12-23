package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;

abstract class IndependentJarIndex extends AbstractJarIndex {
	private final EntryIndex entryIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;

	IndependentJarIndex(
			EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex,
			BridgeMethodIndex bridgeMethodIndex, JarIndexer... otherIndexers
	) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, otherIndexers);

		this.entryIndex = entryIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
	}

	EntryIndex getEntryIndex() {
		return this.entryIndex;
	}

	ReferenceIndex getReferenceIndex() {
		return this.referenceIndex;
	}

	BridgeMethodIndex getBridgeMethodIndex() {
		return this.bridgeMethodIndex;
	}
}

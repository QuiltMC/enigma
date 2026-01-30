package org.quiltmc.enigma.input.records.name_mismatch;

import org.quiltmc.enigma.impl.plugin.RecordIndexingService;

/**
 * {@link #component()} shouldn't be found by {@link RecordIndexingService} - despite being a default getter -
 * because its obf name doesn't match {@link #component}'s.
 */
public record FakeGetterWrongInstructionsRecord(int component) {
	/**
	 * This shouldn't be found by {@link RecordIndexingService} - despite its obf name, access, and descriptor matching
	 * expectations for a getter - because its instructions don't match that of a default getter.
	 */
	public int fakeGetter() {
		return 0;
	}
}

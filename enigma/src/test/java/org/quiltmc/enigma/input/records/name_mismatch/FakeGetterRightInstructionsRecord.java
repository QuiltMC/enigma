package org.quiltmc.enigma.input.records.name_mismatch;

import org.quiltmc.enigma.impl.plugin.RecordIndexingService;

public record FakeGetterRightInstructionsRecord(int component) {
	/**
	 * This <em>should</em> be found by {@link RecordIndexingService} as the getter because it gets the same
	 * obf name as the component field and it has the expected descriptor, access, and instructions as a default getter.
	 *
	 * <p> This behavior is important because it matches decompilers' behavior. Decompilers will consider this a
	 * default getter and hide it, so it's important that we propose a name for it to prevent it from making stats
	 * un-completable.
	 */
	public int fakeGetter() {
		return this.component;
	}
}

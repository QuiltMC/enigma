package org.quiltmc.enigma.input.records.name_mismatch;

import org.quiltmc.enigma.impl.plugin.RecordIndexingService;

public record StringComponentOverrideGetterRecord(String string) {
	/**
	 * This getter should be found by {@link RecordIndexingService} because it's the only getter candidate:
	 * the only other public no-args method returning a {@link String} is {@link #toString()}, and {@code toString} is
	 * no a legal component name.
	 */
	@Override
	public String string() {
		return "";
	}
}

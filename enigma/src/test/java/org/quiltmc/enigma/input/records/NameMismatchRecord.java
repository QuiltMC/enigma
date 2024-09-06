package org.quiltmc.enigma.input.records;

public record NameMismatchRecord(int i) {
	public int a() {
		return 103;
	}

	// obfuscates to b(), mismatching with the record component name
	@Override
	public int i() {
		return this.i;
	}
}

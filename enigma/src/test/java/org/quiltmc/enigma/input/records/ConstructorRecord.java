package org.quiltmc.enigma.input.records;

public record ConstructorRecord(String a, String b, int c, double d) {
	private static final int gaming = 234;

	public ConstructorRecord(String a, String b, double d, int c) {
		this(a, b, c, d);
	}

	public ConstructorRecord(String b) {
		this("gaming", b, 1, 2.0);
	}
}

package org.quiltmc.enigma.input.search_mappings;

public record Obf_d(String a, String b, int c, double d) {
	private static final int gaming = 234;

	public Obf_d(String a, String b, double d, int c) {
		this(a, b, c, d);
	}

	public Obf_d(String b) {
		this("gaming", b, 1, 2.0);
	}
}

package org.quiltmc.enigma.input.records;

public record Record(String a, int b, double c) {
	// non canonical
	public Record(String a) {
		this(a, 0, 0.0);
	}

	// non canonical
	public Record(String a, double c, int b) {
		this(a, b, c);
	}

	// canonical with abnormal bytecode
	public Record(String a, int b, double c) {
		this.a = a;
		this.b = b;
		this.c = c;

		System.out.println("grind");
	}
}

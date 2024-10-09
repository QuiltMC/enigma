package org.quiltmc.enigma.input.interfaces;

public class Inheritor implements Root {
	@Override
	public int a() {
		return 23;
	}

	@Override
	public double b(double c) {
		return c + 100d;
	}
}

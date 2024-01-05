package org.quiltmc.enigma.input.interface_union;

public record Union3Record(double baz) implements AInterface {
	@Override
	public int methodA() {
		return -1;
	}

	@Override
	public void methodFoo() {
	}

	// AInterface -> baz()
}

package org.quiltmc.enigma.input.interface_union;

import java.util.Random;

public class Union1Class implements AInterface, BInterface {
	@Override
	public int methodA() {
		return 32767;
	}

	// AInterface + BInterface
	@Override
	public void methodFoo() {
		System.out.println("foo");
	}

	@Override
	public double baz() {
		return 200;
	}

	@Override
	public double methodB() {
		return new Random().nextGaussian();
	}

	@Override
	public boolean methodBar() {
		return false;
	}
}

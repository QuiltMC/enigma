package org.quiltmc.enigma.input.interface_union;

import java.util.Random;

public class Union2Class extends CClass implements BInterface {
	@Override
	public void methodFoo() {
	}

	@Override
	public double methodB() {
		return 6.02e23;
	}

	@Override
	public boolean methodBar() {
		return new Random().nextExponential() > 2.0;
	}
}

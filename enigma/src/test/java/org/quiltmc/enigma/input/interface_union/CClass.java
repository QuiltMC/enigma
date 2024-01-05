package org.quiltmc.enigma.input.interface_union;

import java.util.Random;

public class CClass {
	// BInterface -> Union2Class
	public boolean methodBar() {
		return true;
	}

	// DInterface -> Union4Class
	public float factor() {
		return (float) new Random().nextGaussian();
	}
}

package org.quiltmc.enigma.input.z_tooltip;

public enum Enums {
	FIRST, SECOND(2),
	THIRD(3), FOURTH(4);

	static {
		System.out.println(FIRST);
		System.out.println(SECOND);
		System.out.println(THIRD);
		System.out.println(FOURTH);
	}

	final int index;

	Enums() {
		this(1);
	}

	Enums(int index) {
		this.index = index;
	}
}

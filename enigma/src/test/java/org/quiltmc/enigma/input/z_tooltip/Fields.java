package org.quiltmc.enigma.input.z_tooltip;

public class Fields {
	static final String STATIC_FIELD = "static field";

	final int initialized = 0;

	Object uninitialized;

	Runnable multiLineField = () -> {
		System.out.println("hello");
		System.out.println("good bye");
	};

	{
		System.out.println(STATIC_FIELD);
		System.out.println(this.initialized);
		System.out.println(this.uninitialized);
		System.out.println(this.multiLineField);
	}
}

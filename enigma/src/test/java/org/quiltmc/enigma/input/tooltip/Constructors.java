package org.quiltmc.enigma.input.tooltip;

public class Constructors {
	public Constructors(String outerArg) {
		new Constructors("outer arg");

		new Methods() {
			@Override
			void abstraction() {
				// tests #252
				System.out.println(outerArg);
			}
		};
	}
}

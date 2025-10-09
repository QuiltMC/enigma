package org.quiltmc.enigma.input.z_tooltip;

public class Constructors {
	public Constructors(String outerArg) {
		new Constructors("outer arg");

		new Methods() {
			// multiline declaration to test unindenting
			@Deprecated
			@Override
			void abstraction() {
				// tests #252
				System.out.println(outerArg);

				if (this == null) {
					this.abstraction();
				}
			}
		};
	}
}

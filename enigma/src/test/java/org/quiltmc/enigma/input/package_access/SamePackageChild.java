package org.quiltmc.enigma.input.package_access;

public class SamePackageChild extends Base {
	class Inner {
		final int value;

		Inner() {
			this.value = SamePackageChild.this.make(); // no synthetic method
		}
	}
}

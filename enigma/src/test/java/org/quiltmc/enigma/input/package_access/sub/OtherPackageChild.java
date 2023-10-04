package org.quiltmc.enigma.input.package_access.sub;

import org.quiltmc.enigma.input.package_access.Base;

public class OtherPackageChild extends Base {
	class Inner {
		final int value;

		Inner() {
			this.value = OtherPackageChild.this.make(); // synthetic method call
		}
	}
}

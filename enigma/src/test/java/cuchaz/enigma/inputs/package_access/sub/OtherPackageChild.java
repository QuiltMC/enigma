package cuchaz.enigma.inputs.package_access.sub;

import cuchaz.enigma.inputs.package_access.Base;

public class OtherPackageChild extends Base {

	class Inner {
		final int value;

		Inner() {
			this.value = OtherPackageChild.this.make(); // synthetic method call
		}
	}
}

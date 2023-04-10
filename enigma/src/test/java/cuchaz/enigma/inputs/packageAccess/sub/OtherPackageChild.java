package cuchaz.enigma.inputs.packageAccess.sub;

import cuchaz.enigma.inputs.packageAccess.Base;

public class OtherPackageChild extends Base {
	class Inner {
		final int value;

		Inner() {
			this.value = OtherPackageChild.this.make(); // synthetic method call
		}
	}
}

package cuchaz.enigma.inputs.packageAccess;

public class SamePackageChild extends Base {
	class Inner {
		final int value;

		Inner() {
			this.value = SamePackageChild.this.make(); // no synthetic method
		}
	}
}

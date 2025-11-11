package org.quiltmc.enigma.input.inner_classes;

@SuppressWarnings("unused")
public class C_ConstructorArgs {
	Inner i;

	public void foo() {
		this.i = new Inner(5);
	}

	class Inner {
		private final int a;

		Inner(int a) {
			this.a = a;
		}
	}
}

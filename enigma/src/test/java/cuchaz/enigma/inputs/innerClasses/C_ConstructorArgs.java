package cuchaz.enigma.inputs.innerClasses;

@SuppressWarnings("unused")
public class C_ConstructorArgs {
	Inner i;

	public void foo() {
		i = new Inner(5);
	}

	class Inner {
		private int a;

		public Inner(int a) {
			this.a = a;
		}
	}
}

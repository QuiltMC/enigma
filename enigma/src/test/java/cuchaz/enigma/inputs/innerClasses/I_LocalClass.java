package cuchaz.enigma.inputs.innerClasses;

@SuppressWarnings("unused")
public class I_LocalClass {
	public void a() {
		class B extends I_LocalClass {
			public void b() {
				System.out.println("b");
			}
		}

		new B().b();
		this.b();
	}

	public void b() {
		System.out.println("B");
	}
}

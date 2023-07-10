package cuchaz.enigma.inputs.validation;

// b
public class SuperClass {
	// a
	private static final String FIELD_A = "Hello";
	// b
	public static final String FIELD_B = "World";
	// a
	private final int fieldA = 0;
	// b
	private int fieldB;
	// b
	public boolean fieldC;

	// c()V
	public static void methodA() {
	}

	// a()Z
	public boolean methodB() {
		return false;
	}

	// a(I)V
	public void methodB(int i) {
	}

	// a()V
	private void methodC() {
	}

	// b()I
	public int methodD() {
		return 0;
	}
}

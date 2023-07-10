package cuchaz.enigma.inputs.validation;

// a
public class BaseClass extends SuperClass {
	// c
	private static final String FIELD_A = "fieldA";
	// a
	public static final String FIELD_B = "fieldB";
	// a
	private final int fieldA = -1;
	// b
	private int fieldB;
	// a
	public boolean fieldC;

	// a()V
	public static void methodA() {
	}

	// b()V
	public void methodE() {
	}

	// a(I)I
	public int methodE(int i) {
		return i;
	}

	// d()V
	private void methodC() {
	}

	// a()I
	public int methodF() {
		return 1;
	}
}

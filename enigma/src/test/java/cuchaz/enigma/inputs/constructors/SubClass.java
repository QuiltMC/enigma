package cuchaz.enigma.inputs.constructors;

// d extends a
public class SubClass extends BaseClass {
	// <init>()V
	public SubClass() {
		// a.<init>()V
	}

	// <init>(I)V
	public SubClass(int num) {
		// <init>()V
		this();
		System.out.println("SubClass " + num);
	}

	// <init>(II)V
	public SubClass(int a, int b) {
		// <init>(I)V
		this(a + b);
	}

	// <init>(III)V
	public SubClass(int a, int b, int c) {
		// a.<init>()V
	}
}

package cuchaz.enigma.inputs.inheritanceTree;

// c extends a
public class SubclassB extends BaseClass {
	// a
	private int numThings;

	// <init>()V
	protected SubclassB() {
		// a.<init>(Ljava/lang/String;)V
		super("B");

		// access to a
		this.numThings = 4;
	}

	@Override
	// a()V
	public void doBaseThings() {
		// call to a.a()Ljava/lang/String;
		System.out.println("Base things by B! " + this.getName());
	}

	// b()V
	public void doBThings() {
		// access to a
		System.out.println("" + this.numThings + " B things!");
	}
}

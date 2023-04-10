package cuchaz.enigma.inputs.inheritanceTree;

// d extends b
public class SubsubclassAA extends SubclassA {
	protected SubsubclassAA() {
		// call to b.<init>(Ljava/lang/String;)V
		super("AA");
	}

	@Override
	// a()Ljava/lang/String;
	public String getName() {
		// call to b.a()Ljava/lang/String;
		return "subsub" + super.getName();
	}

	@Override
	// a()V
	public void doBaseThings() {
		// call to d.a()Ljava/lang/String;
		System.out.println("Base things by " + this.getName());
	}
}

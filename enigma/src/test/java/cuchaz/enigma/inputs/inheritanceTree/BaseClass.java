package cuchaz.enigma.inputs.inheritanceTree;

// a
public abstract class BaseClass {
	// a
	private String name;

	// <init>(Ljava/lang/String;)V
	protected BaseClass(String name) {
		this.name = name;
	}

	// a()Ljava/lang/String;
	public String getName() {
		return this.name;
	}

	// a()V
	public abstract void doBaseThings();
}

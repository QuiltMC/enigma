package cuchaz.enigma.inputs.inheritanceTree;

// b extends a
public abstract class SubclassA extends BaseClass {
	// <init>(Ljava/lang/String;)V
	protected SubclassA(String name) {
		// call to a.<init>(Ljava/lang/String)V
		super(name);
	}
}

package org.quiltmc.enigma.input.interface_union;

public interface AInterface {
	int methodA();

	// BInterface -> Union1Class
	void methodFoo();

	// -> Union3Record
	double baz();
}

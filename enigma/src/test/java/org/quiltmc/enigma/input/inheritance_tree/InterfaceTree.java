package org.quiltmc.enigma.input.inheritance_tree;

public class InterfaceTree {
	interface Root extends Branch12, Branch34 { }

	interface Branch12 extends Leaf1, Leaf2 { }

	interface Branch34 extends Leaf3, Leaf4 { }

	interface Leaf1 { }

	interface Leaf2 { }

	interface Leaf3 { }

	interface Leaf4 { }
}

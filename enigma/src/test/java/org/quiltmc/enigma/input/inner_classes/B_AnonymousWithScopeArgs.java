package org.quiltmc.enigma.input.inner_classes;

public class B_AnonymousWithScopeArgs {
	public static void foo(final D_Simple arg) {
		System.out.println(new Object() {
			@Override
			public String toString() {
				return arg.toString();
			}
		});
	}
}

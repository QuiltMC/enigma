package org.quiltmc.enigma.input.inner_classes;

import java.util.function.Consumer;

public class NestedLambdas {
	public static void main(String g) {
		gaming((s) -> gaming((w) -> {
			System.out.println(s);
			gaming(System.out::println);
		}));
	}

	public static void gaming(Consumer<String> runnable) {
		runnable.accept("Gaming!");
	}
}

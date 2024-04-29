package org.quiltmc.enigma.input;

import java.util.function.Consumer;

public class Keep {
	public static void main(String... args) {
		System.out.println("Keep me!");
	}

	public static void main(String g) {
		gaming((s) -> System.out.println(s));
	}

	public static void gaming(Consumer<String> runnable) {
		runnable.accept("Gaming!");
	}
}

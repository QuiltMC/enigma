package org.quiltmc.enigma.input.lambdas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Lambdas {
	private List<Lambdas> children = new ArrayList<>();

	public Stream<Lambdas> stream() {
		return Stream.concat(Stream.of(this), this.children.stream().flatMap(Lambdas::stream));
	}

	public static void foo() {
		consume(s -> consume(s1 -> {
			System.out.println(s);
			consume(System.out::println);
		}));
	}

	public static void consume(Consumer<String> consumer) {
		consumer.accept("Foo");
	}
}

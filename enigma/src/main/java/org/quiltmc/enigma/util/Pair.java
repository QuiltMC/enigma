package org.quiltmc.enigma.util;

import java.util.Objects;

public record Pair<A, B>(A a, B b) {
	@Override
	public boolean equals(Object o) {
		return o instanceof Pair
				&& Objects.equals(this.a, ((Pair<?, ?>) o).a)
				&& Objects.equals(this.b, ((Pair<?, ?>) o).b);
	}
}

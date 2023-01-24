package cuchaz.enigma.utils;

import java.util.Objects;

public class Pair<A, B> {
    public final A a;
    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.a) * 31 +
               Objects.hashCode(this.b);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pair &&
               Objects.equals(this.a, ((Pair<?, ?>) o).a) &&
               Objects.equals(this.b, ((Pair<?, ?>) o).b);
    }
}

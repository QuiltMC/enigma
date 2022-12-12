package cuchaz.enigma.inputs.bridge;

import java.util.function.Function;
import java.util.function.Supplier;

// b
public class OtherClass implements Supplier<Integer>, Function<String, Integer> {
    // a()Ljava/lang/Integer;
    // bridge get()Ljava/lang/Object;
    @Override
    public Integer get() {
        return -1;
    }

    // a(Ljava/lang/String;)Ljava/lang/Integer;
    // bridge apply(Ljava/lang/Object;)Ljava/lang/Object;
    @Override
    public Integer apply(String s) {
        return s.hashCode();
    }
}

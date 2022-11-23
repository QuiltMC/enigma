package cuchaz.enigma.classprovider;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ClassProvider {
    /**
     * Gets the {@linkplain ClassNode} for a class. The class provider may return a cached result,
     * so it's important to not mutate it.
     *
     * @param name the internal name of the class
     * @return the {@linkplain ClassNode} for that class, or {@code null} if it was not found
     */
    @Nullable
    ClassNode get(String name);

    /**
     * Gets all the classes starting with the given string
     */
    default List<String> getClasses(String prefix) {
        return Collections.emptyList();
    }

    static ClassProvider fromMap(Map<String, ClassNode> classes) {
        return new ClassProvider() {
            @Nullable
            @Override
            public ClassNode get(String name) {
                return classes.get(name);
            }

            @Override
            public List<String> getClasses(String prefix) {
                return classes.keySet().stream().filter(s -> s.startsWith(prefix)).toList();
            }
        };
    }
}

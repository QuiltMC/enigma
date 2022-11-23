package cuchaz.enigma.classprovider;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines a list of {@link ClassProvider}s into one, calling each one in a row
 * until one can provide the class.
 */
public class CombiningClassProvider implements ClassProvider {
    private final ClassProvider[] classProviders;

    public CombiningClassProvider(ClassProvider... classProviders) {
        this.classProviders = classProviders;
    }

    @Override
    @Nullable
    public ClassNode get(String name) {
        for (ClassProvider cp : classProviders) {
            ClassNode node = cp.get(name);

            if (node != null) {
                return node;
            }
        }

        return null;
    }

    @Override
    public List<String> getClasses(String prefix) {
        List<String> classes = new ArrayList<>();
        for (ClassProvider cp : classProviders) {
            classes.addAll(cp.getClasses(prefix));
        }

        return classes;
    }
}

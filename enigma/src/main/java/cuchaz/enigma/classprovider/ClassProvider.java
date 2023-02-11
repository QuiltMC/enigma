package cuchaz.enigma.classprovider;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
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

	Collection<String> getClassNames();

	/**
	 * Gets all the classes in the same root class as the given one.
	 */
	default Collection<String> getClasses(String className) {
		if (className.contains("$")) {
			className = className.substring(0, className.indexOf("$"));
		}

		int depth = className.lastIndexOf('/');
		String finalClassName = className;
		return this.getClassNames()
				.stream()
				.filter(s -> s.lastIndexOf('/') == depth && s.startsWith(finalClassName))
				.toList();
	}

	static ClassProvider fromMap(Map<String, ClassNode> classes) {
		return new ClassProvider() {
			@Nullable
			@Override
			public ClassNode get(String name) {
				return classes.get(name);
			}

			@Override
			public Collection<String> getClassNames() {
				return Collections.unmodifiableSet(classes.keySet());
			}
		};
	}
}

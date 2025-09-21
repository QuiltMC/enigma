package org.quiltmc.enigma.api.class_provider;

import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides java.* classes.
 */
public class JavaClassProvider implements ClassProvider {
	private static final String CLASS_EXTENSION = ".class";
	private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("^java/.*" + Pattern.quote(CLASS_EXTENSION) + "$");

	private static final ImmutableSet<String> UN_ANALYZABLE_CLASS_NAMES = ImmutableSet.of(
			"java/lang/module/ModuleDescriptor",
			"java/io/PrintStream"
	);

	@Nullable
	private Set<String> classes;

	@Nullable
	@Override
	public ClassNode get(String name) {
		try (InputStream in = Object.class.getResourceAsStream("/" + name + CLASS_EXTENSION)) {
			if (in == null) {
				return null;
			}

			ClassNode node = new ClassNode();
			new ClassReader(in).accept(node, 0);
			return node;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Collection<String> getClassNames() {
		if (this.classes == null) {
			this.classes = Object.class.getModule().getLayer().configuration().modules().stream()
				.map(ResolvedModule::reference)
				.flatMap(ref -> {
					try (ModuleReader reader = ref.open()) {
						return reader.list();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.filter(resource -> JAVA_CLASS_PATTERN.matcher(resource).find())
				.map(javaClass -> javaClass.substring(0, javaClass.length() - CLASS_EXTENSION.length()))
				// HACK: these cause AnalyzerException's
				.filter(className -> !UN_ANALYZABLE_CLASS_NAMES.contains(className))
				.filter(className -> this.get(className) != null)
				.collect(Collectors.toSet());
		}

		return this.classes;
	}
}

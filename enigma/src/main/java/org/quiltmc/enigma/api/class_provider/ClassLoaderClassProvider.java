package org.quiltmc.enigma.api.class_provider;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.util.AsmUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Collection;

public class ClassLoaderClassProvider implements ClassProvider {
	private final ClassLoader loader;

	public ClassLoaderClassProvider(ClassLoader loader) {
		this.loader = loader;
	}

	@Nullable
	@Override
	@SuppressWarnings("ConstantConditions")
	public ClassNode get(String name) {
		try {
			Class<?> clazz = this.loader.loadClass(name);
			String className = clazz.getName();
			int i = className.lastIndexOf('.');
			String resourceName = className.substring(i != -1 ? i + 1 : 0) + ".class";

			try (var resource = clazz.getResourceAsStream(resourceName)) {
				return AsmUtil.bytesToNode(resource.readAllBytes());
			} catch (IOException ignored) {}
		} catch (ClassNotFoundException ignored) {}

		return null;
	}

	@Override
	public Collection<String> getClassNames() {
		Instrumentation instrumentation;
		try {
			instrumentation = ByteBuddyAgent.getInstrumentation();
		} catch (Exception e) {
			ByteBuddyAgent.install();
			instrumentation = ByteBuddyAgent.getInstrumentation();
		}

		// really cursed regex that makes sure all names are actual classes
		// instrumentation gives us some data like "[B" and "[Ljava.lang.Throwable" in addition to the real data, and this is the easiest way to filter
		return Arrays.stream(instrumentation.getInitiatedClasses(this.loader)).map(Class::getName).filter(name -> name.matches("([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*")).toList();
	}
}

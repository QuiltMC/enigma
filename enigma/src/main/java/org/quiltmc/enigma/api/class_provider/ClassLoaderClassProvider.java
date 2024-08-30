package org.quiltmc.enigma.api.class_provider;

import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.util.AsmUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
		// todo implement
		return List.of("java.lang.Object", "java.lang.Record");
	}
}

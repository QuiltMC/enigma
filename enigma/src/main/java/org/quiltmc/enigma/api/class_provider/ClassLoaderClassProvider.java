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
	public ClassNode get(String name) {
		if (this.loader.getResource(name) == null) {
			return null;
		}

		try (var resource = this.loader.getResourceAsStream(name)) {
			assert resource != null;
			// todo doesn't work!
			return AsmUtil.bytesToNode(resource.readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> getClassNames() {
		// todo implement
		return List.of("java/lang/Record");
	}
}

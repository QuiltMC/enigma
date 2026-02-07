package org.quiltmc.enigma.impl.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;

import java.util.Set;

public class ParamSyntheticFieldIndexingService implements JarIndexerService {
	public static final String ID = "enigma:param_synthetic_field_indexer";

	private final ParamSyntheticFieldIndexingVisitor visitor;

	ParamSyntheticFieldIndexingService(ParamSyntheticFieldIndexingVisitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
		for (final String className : scope) {
			final ClassNode node = classProvider.get(className);
			if (node != null) {
				node.accept(this.visitor);
			}
		}
	}

	@Override
	public String getId() {
		return ID;
	}
}

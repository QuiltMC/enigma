package org.quiltmc.enigma.api.service;

import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.objectweb.asm.ClassVisitor;

import java.util.Set;

/**
 * Jar indexer services analyse jar files as they're opened to collect information about their contents.
 * <br>
 * Jar indexer services are not active by default, and need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface JarIndexerService extends EnigmaService {
	EnigmaServiceType<JarIndexerService> TYPE = new EnigmaServiceType<>("jar_indexer", false);

	void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex);

	static JarIndexerService fromVisitor(ClassVisitor visitor, String id) {
		return new JarIndexerService() {
			@Override
			public void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex) {
				for (String className : scope) {
					ClassNode node = classProvider.get(className);
					if (node != null) {
						node.accept(visitor);
					}
				}
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}
}

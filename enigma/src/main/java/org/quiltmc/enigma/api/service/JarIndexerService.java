package org.quiltmc.enigma.api.service;

import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.objectweb.asm.ClassVisitor;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;

/**
 * Jar indexer services analyse jar files as they're opened to collect information about their contents.
 * <br>
 * Jar indexer services are not active by default, and need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface JarIndexerService extends EnigmaService {
	EnigmaServiceType<JarIndexerService> TYPE = EnigmaServiceType.create("jar_indexer");

	/**
	 * Indexes a collection of classes.
	 * @param classProvider a provider to translate class names into {@link ClassNode class nodes}.
	 * @param jarIndex the current jar index
	 */
	void acceptJar(ProjectClassProvider classProvider, JarIndex jarIndex);

	/**
	 * Creates an indexer service that runs all {@link ClassNode class nodes} through the provided {@link ClassVisitor visitor}.
	 * @param visitor the visitor to pass classes through
	 * @param id the service's ID
	 * @return the indexer service
	 */
	static JarIndexerService fromVisitor(ClassVisitor visitor, String id) {
		return new JarIndexerService() {
			@Override
			public void acceptJar(ProjectClassProvider classProvider, JarIndex jarIndex) {
				for (String className : classProvider.getClassNames()) {
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

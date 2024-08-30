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
	EnigmaServiceType<JarIndexerService> TYPE = EnigmaServiceType.create("jar_indexer");

	static boolean shouldIndexLibraries(EnigmaServiceContext<JarIndexerService> context) {
		return context.getSingleArgument("index_libraries").map(Boolean::parseBoolean).orElse(false);
	}

	/**
	 * Indexes a collection of classes.
	 * @param scope a list of class names to be indexed
	 * @param classProvider a provider to translate class names into {@link ClassNode class nodes}
	 * @param jarIndex the current jar index
	 */
	void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex);

	/**
	 * Whether this indexer should be run on libraries in addition to the main project being indexed.
	 * @implNote implementations should use {@link #shouldIndexLibraries(EnigmaServiceContext)} to allow changing this setting via the {@link org.quiltmc.enigma.api.EnigmaProfile profile}
	 * @return whether this indexer should target libraries
	 */
	default boolean shouldIndexLibraries() {
		return false;
	}

	/**
	 * Creates an indexer service that runs all {@link ClassNode class nodes} through the provided {@link ClassVisitor visitor}.
	 * @param context the profile context for the service
	 * @param visitor the visitor to pass classes through
	 * @param id the service's ID
	 * @return the indexer service
	 */
	static JarIndexerService fromVisitor(EnigmaServiceContext<JarIndexerService> context, ClassVisitor visitor, String id) {
		boolean indexLibs = shouldIndexLibraries(context);

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

			@Override
			public boolean shouldIndexLibraries() {
				return indexLibs;
			}
		};
	}
}

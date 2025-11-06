package org.quiltmc.enigma.api.service;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;

import java.util.Set;

/**
 * Jar indexer services analyse jar files as they're opened to collect information about their contents.
 * <br>
 * Jar indexer services are not active by default, and need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface JarIndexerService extends EnigmaService {
	EnigmaServiceType<JarIndexerService> TYPE = EnigmaServiceType.create("jar_indexer");

	/**
	 * Checks the {@code index_libraries} argument in the context to determine if libraries should be indexed.
	 * @param context the context for this service
	 * @return whether libraries should be indexed
	 */
	static boolean shouldIndexLibraries(@Nullable EnigmaServiceContext<JarIndexerService> context) {
		if (context == null) {
			return false;
		}

		return context.getSingleArgument("index_libraries").map(Boolean::parseBoolean).orElse(false);
	}

	/**
	 * Indexes a collection of classes.
	 * @param scope the current scope to be indexed. This is either all main jar classes or all library classes
	 * @param classProvider a provider to translate class names into {@link ClassNode class nodes}. Contains both library and main jar classes
	 * @param jarIndex the current jar index
	 */
	void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex);

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
	 * @param visitor the visitor to pass classes through
	 * @param id the service's ID
	 * @return the indexer service
	 */
	static JarIndexerService fromVisitor(ClassVisitor visitor, String id) {
		return fromVisitor(null, visitor, id);
	}

	/**
	 * Creates an indexer service that runs all {@link ClassNode class nodes} through the provided {@link ClassVisitor visitor}.
	 * Overrides {@link #shouldIndexLibraries()} according to the profile argument described in {@link #shouldIndexLibraries(EnigmaServiceContext)}.
	 * @param context the profile context for the service
	 * @param visitor the visitor to pass classes through
	 * @param id the service's ID
	 * @return the indexer service
	 */
	static JarIndexerService fromVisitor(@Nullable EnigmaServiceContext<JarIndexerService> context, ClassVisitor visitor, String id) {
		boolean indexLibs = shouldIndexLibraries(context);

		return new JarIndexerService() {
			@Override
			public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
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

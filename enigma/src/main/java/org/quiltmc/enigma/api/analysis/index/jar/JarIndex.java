package org.quiltmc.enigma.api.analysis.index.jar;

import com.google.common.collect.ListMultimap;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;

import java.util.Set;

public interface JarIndex extends JarIndexer {
	/**
	 * Gets the index associated with the provided class.
	 * @param clazz the class of the index desired - for example, {@code PackageIndex.class}
	 * @return the index
	 */
	<T extends JarIndexer> T getIndex(Class<T> clazz);

	/**
	 * Runs every configured indexer over the provided jar.
	 * @param classNames the obfuscated names of each class in the jar
	 * @param classProvider a class provider containing all classes in the jar
	 * @param progress a progress listener to track index completion
	 */
	void indexJar(Set<String> classNames, ClassProvider classProvider, ProgressListener progress);

	/**
	 * {@return an entry resolver with this index's contents as context}
	 */
	EntryResolver getEntryResolver();

	/**
	 * {@return a map of all entries, keyed by their class}
	 */
	ListMultimap<ClassEntry, ParentedEntry<?>> getChildrenByClass();

	/**
	 * @param internalName
	 * {@return whether this class is included in this index}
	 */
	boolean isIndexed(String internalName);
}

package org.quiltmc.enigma.api;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.impl.EnigmaProjectImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Represents an Enigma project which applies a set of mappings to a source jar.
 *
 * @implNote This interface is not intended for implementation by api users. An instance can be created using the
 * {@link #of} factory method.
 */
public interface EnigmaProject {
	// TODO should this be api?
	static EnigmaProject of(Enigma enigma, Path jarPath, ClassProvider classProvider, JarIndex jarIndex, JarIndex libIndex, JarIndex combinedIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> proposedNames, byte[] jarChecksum) {
		return new EnigmaProjectImpl(enigma, jarPath, classProvider, jarIndex, libIndex, combinedIndex, mappingsIndex, proposedNames, jarChecksum);
	}

	/**
	 * Sets the current mappings of this project.
	 * Note that this triggers both an index of the mappings and dynamic name proposal, which may be expensive.
	 * @param mappings the new mappings
	 * @param progress a progress listener for indexing
	 */
	void setMappings(@Nullable EntryTree<EntryMapping> mappings, ProgressListener progress);

	Enigma getEnigma();

	Path getJarPath();

	ClassProvider getClassProvider();

	/**
	 * Gets the index of the main jar of this project; the jar being mapped.
	 */
	JarIndex getJarIndex();

	/**
	 * Gets the index of the library jars of this project.
	 */
	JarIndex getLibIndex();

	/**
	 * Gets the index of the main jar <em>and</em> library jars of this project.
	 */
	JarIndex getCombinedIndex();

	MappingsIndex getMappingsIndex();

	// TODO should this be api?
	byte[] getJarChecksum();

	EntryRemapper getRemapper();

	Collection<Entry<?>> dropMappings(ProgressListener progress);

	boolean isNavigable(Entry<?> obfEntry);

	boolean isRenamable(Entry<?> obfEntry);

	boolean isRenamable(EntryReference<Entry<?>, Entry<?>> obfReference);

	boolean isObfuscated(Entry<?> entry);

	boolean isSynthetic(Entry<?> entry);

	boolean isAnonymousOrLocal(ClassEntry classEntry);

	/**
	 * Verifies that the provided {@code parameter} has a valid index for its parent method.
	 * This method validates both the upper and lower bounds of the parent method's index range.
	 *
	 * <p>Note that this method could still return {@code true} for an invalid index in the case that the index is impossible due to double-size parameters --
	 * for example, if the index is 4 and there's a double at index 3, the index would be invalid.
	 * But honestly, we at <a href=https://quiltmc.org>QuiltMC</a> doubt that that's a situation you'll ever be running into.
	 * If it is, complain <a href=https://github.com/QuiltMC/enigma/issues>in our issue tracker</a> about us writing this whole comment instead of implementing that functionality.
	 *
	 * @param parameter the parameter to validate
	 * @return whether the index is valid
	 */
	boolean validateParameterIndex(LocalVariableEntry parameter);

	JarExport exportRemappedJar(ProgressListener progress);

	interface JarExport {
		void write(Path path, ProgressListener progress) throws IOException;

		SourceExport decompile(
				ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy
		);

		Stream<? extends ClassSource> decompileStream(
				ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy
		);
	}

	interface SourceExport {
		void write(Path path, ProgressListener progress) throws IOException;
	}

	interface ClassSource {
		void writeTo(Path path) throws IOException;

		Path resolvePath(Path root);
	}

	enum DecompileErrorStrategy {
		PROPAGATE,
		TRACE_AS_SOURCE,
		IGNORE
	}
}

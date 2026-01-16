package org.quiltmc.enigma.api.analysis.index.jar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;

public sealed interface EntryIndex extends JarIndexer permits CombinedEntryIndex, IndependentEntryIndex {
	boolean hasClass(ClassEntry entry);

	boolean hasMethod(MethodEntry entry);

	boolean hasParameter(LocalVariableEntry entry);

	boolean hasField(FieldEntry entry);

	/**
	 * Checks whether the entry has been indexed and therefore exists in the JAR file.
	 * <br>
	 * For parameters, checks the parent method and partially verifies their indices.
	 * To fully validate a parameter's index, use {@link EnigmaProject#validateParameterIndex(LocalVariableEntry)}.
	 *
	 * @param entry the entry to check
	 * @return whether the entry exists
	 */
	boolean hasEntry(Entry<?> entry);

	/**
	 * Validates that the parameter index is not below the minimum index for its parent method and therefore could be valid.
	 * <br>Note that this method does not guarantee the index is valid -- for full validation, call {@link EnigmaProject#validateParameterIndex(LocalVariableEntry)}.
	 *
	 * @param parameter the parameter to validate
	 * @return whether the index could be valid
	 * @see EnigmaProject#validateParameterIndex(LocalVariableEntry)
	 */
	boolean validateParameterIndex(LocalVariableEntry parameter);

	@Nullable
	AccessFlags getMethodAccess(MethodEntry entry);

	@Nullable
	AccessFlags getParameterAccess(LocalVariableEntry entry);

	@Nullable
	AccessFlags getFieldAccess(FieldEntry entry);

	@Nullable
	AccessFlags getClassAccess(ClassEntry entry);

	@Nullable
	AccessFlags getEntryAccess(Entry<?> entry);

	@Nullable
	ClassDefEntry getDefinition(ClassEntry entry);

	@Nullable
	MethodDefEntry getDefinition(MethodEntry entry);

	@Nullable
	LocalVariableDefEntry getDefinition(LocalVariableEntry entry);

	@Nullable
	FieldDefEntry getDefinition(FieldEntry entry);

	Collection<ClassEntry> getClasses();

	Collection<MethodEntry> getMethods();

	Collection<LocalVariableEntry> getParameters();

	Collection<FieldEntry> getFields();

	/**
	 * Returns all indexed entries, organised into an {@link EntryTree}.
	 * Note that all entries will have their mapping set to {@code null}.
	 *
	 * @return the entry tree
	 */
	EntryTree<EntryMapping> getTree();

	@Override
	default Class<? extends JarIndexer> getType() {
		return EntryIndex.class;
	}

	@Override
	default String getTranslationKey() {
		return "progress.jar.indexing.process.entries";
	}
}

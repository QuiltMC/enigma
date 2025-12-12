package org.quiltmc.enigma.api.analysis.index.jar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.MergedEntryMappingTree;
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
import org.quiltmc.enigma.util.CombinedCollection;

import java.util.Collection;

final class CombinedEntryIndex implements EntryIndex {
	private final EntryIndex mainIndex;
	private final EntryIndex libIndex;

	private final Collection<ClassEntry> classes;
	private final Collection<MethodEntry> methods;
	private final Collection<LocalVariableEntry> parameters;
	private final Collection<FieldEntry> fields;

	/**
	 * Lazily populated cache.
	 *
	 * @see #getTree()
	 */
	@Nullable
	private EntryTree<EntryMapping> tree;

	CombinedEntryIndex(EntryIndex mainIndex, EntryIndex libIndex) {
		this.mainIndex = mainIndex;
		this.libIndex = libIndex;

		this.classes = new CombinedCollection<>(this.mainIndex.getClasses(), this.libIndex.getClasses());
		this.methods = new CombinedCollection<>(this.mainIndex.getMethods(), this.libIndex.getMethods());
		this.parameters = new CombinedCollection<>(this.mainIndex.getParameters(), this.libIndex.getParameters());
		this.fields = new CombinedCollection<>(this.mainIndex.getFields(), this.libIndex.getFields());
	}

	@Override
	public boolean hasClass(ClassEntry entry) {
		return this.mainIndex.hasClass(entry) || this.libIndex.hasClass(entry);
	}

	@Override
	public boolean hasMethod(MethodEntry entry) {
		return this.mainIndex.hasMethod(entry) || this.libIndex.hasMethod(entry);
	}

	@Override
	public boolean hasParameter(LocalVariableEntry entry) {
		return this.mainIndex.hasParameter(entry) || this.libIndex.hasParameter(entry);
	}

	@Override
	public boolean hasField(FieldEntry entry) {
		return this.mainIndex.hasField(entry) || this.libIndex.hasField(entry);
	}

	@Override
	public boolean hasEntry(Entry<?> entry) {
		return this.mainIndex.hasEntry(entry) || this.libIndex.hasEntry(entry);
	}

	@Override
	public boolean validateParameterIndex(LocalVariableEntry parameter) {
		return this.mainIndex.validateParameterIndex(parameter) || this.libIndex.validateParameterIndex(parameter);
	}

	@Override
	public @Nullable AccessFlags getMethodAccess(MethodEntry entry) {
		final AccessFlags mainAccess = this.mainIndex.getMethodAccess(entry);
		return mainAccess == null ? this.libIndex.getMethodAccess(entry) : mainAccess;
	}

	@Override
	public @Nullable AccessFlags getParameterAccess(LocalVariableEntry entry) {
		final AccessFlags mainAccess = this.mainIndex.getParameterAccess(entry);
		return mainAccess == null ? this.libIndex.getParameterAccess(entry) : mainAccess;
	}

	@Override
	public @Nullable AccessFlags getFieldAccess(FieldEntry entry) {
		final AccessFlags mainAccess = this.mainIndex.getFieldAccess(entry);
		return mainAccess == null ? this.libIndex.getFieldAccess(entry) : mainAccess;
	}

	@Override
	public @Nullable AccessFlags getClassAccess(ClassEntry entry) {
		final AccessFlags mainAccess = this.mainIndex.getClassAccess(entry);
		return mainAccess == null ? this.libIndex.getClassAccess(entry) : mainAccess;
	}

	@Override
	public @Nullable AccessFlags getEntryAccess(Entry<?> entry) {
		final AccessFlags mainAccess = this.mainIndex.getEntryAccess(entry);
		return mainAccess == null ? this.libIndex.getEntryAccess(entry) : mainAccess;
	}

	@Override
	public @Nullable ClassDefEntry getDefinition(ClassEntry entry) {
		final ClassDefEntry mainDef = this.mainIndex.getDefinition(entry);
		return mainDef == null ? this.libIndex.getDefinition(entry) : mainDef;
	}

	@Override
	public @Nullable MethodDefEntry getDefinition(MethodEntry entry) {
		final MethodDefEntry mainDef = this.mainIndex.getDefinition(entry);
		return mainDef == null ? this.libIndex.getDefinition(entry) : mainDef;
	}

	@Override
	public @Nullable LocalVariableDefEntry getDefinition(LocalVariableEntry entry) {
		final LocalVariableDefEntry mainDef = this.mainIndex.getDefinition(entry);
		return mainDef == null ? this.libIndex.getDefinition(entry) : mainDef;
	}

	@Override
	public @Nullable FieldDefEntry getDefinition(FieldEntry entry) {
		final FieldDefEntry mainDef = this.mainIndex.getDefinition(entry);
		return mainDef == null ? this.libIndex.getDefinition(entry) : mainDef;
	}

	@Override
	public Collection<ClassEntry> getClasses() {
		return this.classes;
	}

	@Override
	public Collection<MethodEntry> getMethods() {
		return this.methods;
	}

	@Override
	public Collection<LocalVariableEntry> getParameters() {
		return this.parameters;
	}

	@Override
	public Collection<FieldEntry> getFields() {
		return this.fields;
	}

	@Override
	public EntryTree<EntryMapping> getTree() {
		if (this.tree == null) {
			this.tree = new MergedEntryMappingTree(this.mainIndex.getTree(), this.libIndex.getTree());
		}

		return this.tree;
	}
}

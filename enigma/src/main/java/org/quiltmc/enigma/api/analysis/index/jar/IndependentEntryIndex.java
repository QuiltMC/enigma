package org.quiltmc.enigma.api.analysis.index.jar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
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
import java.util.HashMap;
import java.util.Map;

class IndependentEntryIndex implements EntryIndex {
	private final EntryTree<EntryMapping> tree = new HashEntryTree<>();

	private final Map<FieldEntry, FieldDefEntry> fieldDefinitions = new HashMap<>();
	private final Map<MethodEntry, MethodDefEntry> methodDefinitions = new HashMap<>();
	private final Map<ClassEntry, ClassDefEntry> classDefinitions = new HashMap<>();
	private final Map<LocalVariableEntry, LocalVariableDefEntry> parameterDefinitions = new HashMap<>();

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		this.classDefinitions.put(classEntry, classEntry);
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		this.methodDefinitions.put(methodEntry, methodEntry);
		methodEntry.streamParameters(this).forEach(paramEntry ->
				this.parameterDefinitions.put(paramEntry, paramEntry)
		);
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		this.fieldDefinitions.put(fieldEntry, fieldEntry);
	}

	@Override
	public void processIndex(JarIndex index) {
		for (ClassEntry entry : this.getClasses()) {
			this.tree.insert(entry, null);
		}

		for (FieldEntry entry : this.getFields()) {
			this.tree.insert(entry, null);
		}

		for (MethodEntry entry : this.getMethods()) {
			this.tree.insert(entry, null);
		}

		for (LocalVariableEntry entry : this.getParameters()) {
			this.tree.insert(entry, null);
		}
	}

	@Override
	public boolean hasClass(ClassEntry entry) {
		return this.classDefinitions.containsKey(entry);
	}

	@Override
	public boolean hasMethod(MethodEntry entry) {
		return this.methodDefinitions.containsKey(entry);
	}

	@Override
	public boolean hasParameter(LocalVariableEntry entry) {
		return this.parameterDefinitions.containsKey(entry);
	}

	@Override
	public boolean hasField(FieldEntry entry) {
		return this.fieldDefinitions.containsKey(entry);
	}

	@Override
	public boolean hasEntry(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			return this.hasClass(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return this.hasMethod(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return this.hasField(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			if (this.hasParameter(localVariableEntry)) {
				return this.validateParameterIndex(localVariableEntry);
			}
		}

		return false;
	}

	@Override
	public boolean validateParameterIndex(LocalVariableEntry parameter) {
		MethodEntry parent = parameter.getParent();
		AccessFlags parentAccess = this.getMethodAccess(parent);

		int startIndex = parentAccess != null && parentAccess.isStatic() ? 0 : 1;
		return parameter.getIndex() >= startIndex;
	}

	@Override
	public @Nullable AccessFlags getMethodAccess(MethodEntry entry) {
		var def = this.methodDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Override
	public @Nullable AccessFlags getParameterAccess(LocalVariableEntry entry) {
		var def = this.parameterDefinitions.get(entry);
		return def == null ? null : this.getMethodAccess(def.getParent());
	}

	@Override
	public @Nullable AccessFlags getFieldAccess(FieldEntry entry) {
		var def = this.fieldDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Override
	public @Nullable AccessFlags getClassAccess(ClassEntry entry) {
		var def = this.classDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Override
	public @Nullable AccessFlags getEntryAccess(Entry<?> entry) {
		if (entry instanceof MethodEntry methodEntry) {
			return this.getMethodAccess(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return this.getFieldAccess(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			return this.getParameterAccess(localVariableEntry);
		} else if (entry instanceof ClassEntry classEntry) {
			return this.getClassAccess(classEntry);
		}

		return null;
	}

	@Override
	public @Nullable ClassDefEntry getDefinition(ClassEntry entry) {
		return this.classDefinitions.get(entry);
	}

	@Override
	public @Nullable MethodDefEntry getDefinition(MethodEntry entry) {
		return this.methodDefinitions.get(entry);
	}

	@Override
	public @Nullable LocalVariableDefEntry getDefinition(LocalVariableEntry entry) {
		return this.parameterDefinitions.get(entry);
	}

	@Override
	public @Nullable FieldDefEntry getDefinition(FieldEntry entry) {
		return this.fieldDefinitions.get(entry);
	}

	@Override
	public Collection<ClassEntry> getClasses() {
		return this.classDefinitions.keySet();
	}

	@Override
	public Collection<MethodEntry> getMethods() {
		return this.methodDefinitions.keySet();
	}

	@Override
	public Collection<LocalVariableEntry> getParameters() {
		return this.parameterDefinitions.keySet();
	}

	@Override
	public Collection<FieldEntry> getFields() {
		return this.fieldDefinitions.keySet();
	}

	@Override
	public EntryTree<EntryMapping> getTree() {
		return this.tree;
	}
}

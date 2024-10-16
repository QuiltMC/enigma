package org.quiltmc.enigma.api.analysis.index.jar;

import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntryIndex implements JarIndexer {
	private final EntryTree<EntryMapping> tree = new HashEntryTree<>();

	private final Map<FieldEntry, FieldDefEntry> fieldDefinitions = new HashMap<>();
	private final Map<MethodEntry, MethodDefEntry> methodDefinitions = new HashMap<>();
	private final Map<ClassEntry, ClassDefEntry> classDefinitions = new HashMap<>();

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		this.classDefinitions.put(classEntry, classEntry);
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		this.methodDefinitions.put(methodEntry, methodEntry);
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
	}

	public boolean hasClass(ClassEntry entry) {
		return this.classDefinitions.containsKey(entry);
	}

	public boolean hasMethod(MethodEntry entry) {
		return this.methodDefinitions.containsKey(entry);
	}

	public boolean hasField(FieldEntry entry) {
		return this.fieldDefinitions.containsKey(entry);
	}

	/**
	 * Checks whether the entry has been indexed and therefore exists in the JAR file.
	 * <br>
	 * Parameters are not indexed, and this method does not fully verify validity of parameter indices.
	 * Therefore, it is possible that this method returns {@code true} for an invalid parameter.
	 * @param entry the entry to check
	 * @return whether the entry exists
	 * @see #hasEntry(Entry, EnigmaProject)
	 */
	public boolean hasEntry(Entry<?> entry) {
		return this.hasEntry(entry, null);
	}

	/**
	 * Checks whether the entry has been indexed and therefore exists in the JAR file.
	 * <br>
	 * For parameters, which are not indexed, verifies that they have a valid index and therefore could exist.
	 * @param entry the entry to check
	 * @param project the current project
	 * @return whether the entry exists
	 */
	@SuppressWarnings("ConstantConditions")
	public boolean hasEntry(Entry<?> entry, @Nullable EnigmaProject project) {
		if (entry instanceof ClassEntry classEntry) {
			return this.hasClass(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return this.hasMethod(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return this.hasField(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			MethodEntry parent = localVariableEntry.getParent();
			if (this.hasMethod(parent)) {
				AtomicInteger maxLocals = new AtomicInteger(-1);
				ClassEntry parentClass = parent != null ? parent.getParent() : null;

				if (project != null) {
					// find max_locals for method, representing the number of parameters it receives (JVMS§4.7.3)
					// note: parent class cannot be null, warning suppressed
					ClassNode classNode = project.getClassProvider().get(parentClass.getFullName());
					if (classNode != null) {
						classNode.methods.stream()
							.filter(node -> node.name.equals(parent.getName()) && node.desc.equals(parent.getDesc().toString()))
							.findFirst().ifPresent(node -> maxLocals.set(node.maxLocals));
					}
				}

				AccessFlags parentAccess = this.getMethodAccess(parent);
				int startIndex = parentAccess != null && parentAccess.isStatic() ? 0 : 1;

				// if maxLocals is -1 it's not found for the method and should be ignored
				return localVariableEntry.getIndex() >= startIndex && (maxLocals.get() == -1 || localVariableEntry.getIndex() <= maxLocals.get() - 1);
			}
		}

		return false;
	}

	@Nullable
	public AccessFlags getMethodAccess(MethodEntry entry) {
		var def = this.methodDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Nullable
	public AccessFlags getFieldAccess(FieldEntry entry) {
		var def = this.fieldDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Nullable
	public AccessFlags getClassAccess(ClassEntry entry) {
		var def = this.classDefinitions.get(entry);
		return def == null ? null : def.getAccess();
	}

	@Nullable
	public AccessFlags getEntryAccess(Entry<?> entry) {
		if (entry instanceof MethodEntry methodEntry) {
			return this.getMethodAccess(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return this.getFieldAccess(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			return this.getMethodAccess(localVariableEntry.getParent());
		} else if (entry instanceof ClassEntry classEntry) {
			return this.getClassAccess(classEntry);
		}

		return null;
	}

	@Nullable
	public ClassDefEntry getDefinition(ClassEntry entry) {
		return this.classDefinitions.get(entry);
	}

	@Nullable
	public MethodDefEntry getDefinition(MethodEntry entry) {
		return this.methodDefinitions.get(entry);
	}

	@Nullable
	public FieldDefEntry getDefinition(FieldEntry entry) {
		return this.fieldDefinitions.get(entry);
	}

	public Collection<ClassEntry> getClasses() {
		return this.classDefinitions.keySet();
	}

	public Collection<MethodEntry> getMethods() {
		return this.methodDefinitions.keySet();
	}

	public Collection<FieldEntry> getFields() {
		return this.fieldDefinitions.keySet();
	}

	/**
	 * Returns all indexed entries, organised into an {@link EntryTree}.
	 * Note that all entries will have their mapping set to {@code null}.
	 * @return the entry tree
	 */
	public EntryTree<EntryMapping> getTree() {
		return this.tree;
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.entries";
	}
}

package org.quiltmc.enigma.impl.translation.mapping;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

public class MappingsChecker {
	private final EnigmaProject project;
	private final JarIndex index;
	private final EntryTree<EntryMapping> mappings;

	public MappingsChecker(EnigmaProject project, JarIndex index, EntryTree<EntryMapping> mappings) {
		this.project = project;
		this.index = index;
		this.mappings = mappings;
	}

	private Dropper collectMappings(ProgressListener progress, Dropper dropper, BiConsumer<Dropper, Entry<?>> dropFunction) {
		// HashEntryTree#getAllEntries filters out empty classes
		List<? extends Entry<?>> entries = new ArrayList<>(StreamSupport.stream(this.mappings.spliterator(), false).map(EntryTreeNode::getEntry).toList());

		// sort so that we begin with local variables and end with class entries. this is probably a terrible way of doing this
		entries.sort((a, b) -> {
			if (a instanceof LocalVariableEntry && !(b instanceof LocalVariableEntry)) {
				return -1;
			} else if (b instanceof LocalVariableEntry && !(a instanceof LocalVariableEntry)) {
				return 1;
			} else if (a instanceof ClassEntry && !(b instanceof ClassEntry)) {
				return 1;
			} else if (b instanceof ClassEntry && !(a instanceof ClassEntry)) {
				return -1;
			}

			return 0;
		});

		progress.init(entries.size(), "Checking for dropped mappings");

		int steps = 0;
		for (Entry<?> entry : entries) {
			progress.step(steps++, entry.toString());
			dropFunction.accept(dropper, entry);
		}

		return dropper;
	}

	public Dropper collectBrokenMappings(ProgressListener progress, Dropper dropper) {
		return this.collectMappings(progress, dropper, this::tryDropBrokenEntry);
	}

	private void tryDropBrokenEntry(Dropper dropper, Entry<?> entry) {
		if (this.shouldDropBrokenEntry(dropper, entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropper.addPendingDrop(entry, mapping);
			}
		}
	}

	private boolean shouldDropBrokenEntry(Dropper dropper, Entry<?> entry) {
		if (!this.index.getIndex(EntryIndex.class).hasEntry(entry)) {
			return true;
		}

		Collection<Entry<?>> resolvedEntries = this.index.getEntryResolver().resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT);

		if (resolvedEntries.isEmpty()) {
			// Entry doesn't exist at all, drop it.
			return true;
		} else if (resolvedEntries.contains(entry)) {
			// Entry is the root, don't drop it.
			return false;
		}

		// Method entry has parameter names, keep it even though it's not the root.
		return !(entry instanceof MethodEntry) || this.hasNoMappedChildren(entry, dropper);

		// Entry is not the root, and is not a method with params
	}

	public Dropper collectEmptyMappings(ProgressListener progress, Dropper dropperBroken) {
		return this.collectMappings(progress, dropperBroken, this::tryDropEmptyEntry);
	}

	private void tryDropEmptyEntry(Dropper dropper, Entry<?> entry) {
		if (this.shouldDropEmptyMapping(dropper, entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropper.addPendingDrop(entry, mapping);
			}
		}
	}

	private boolean shouldDropEmptyMapping(Dropper dropper, Entry<?> entry) {
		EntryMapping mapping = this.mappings.get(entry);
		if (mapping != null) {
			boolean isEmpty = (mapping.targetName() == null && mapping.javadoc() == null) || !this.project.isRenamable(entry);

			if (isEmpty) {
				return this.hasNoMappedChildren(entry, dropper);
			}
		}

		return false;
	}

	private boolean hasNoMappedChildren(Entry<?> entry, Dropper dropper) {
		var children = this.mappings.getChildren(entry);

		// account for child mappings that have been dropped already
		if (!children.isEmpty()) {
			var droppedMappings = dropper.getDroppedAndPending();

			for (Entry<?> child : children) {
				var mapping = this.mappings.get(child);
				if ((!droppedMappings.containsKey(child)
						&& mapping != null && mapping.tokenType() != TokenType.OBFUSCATED)
						|| !this.hasNoMappedChildren(child, dropper)) {
					return false;
				}
			}
		}

		return true;
	}

	public static class Dropper {
		private final Map<Entry<?>, String> droppedMappings = new HashMap<>();
		private final Map<Entry<?>, String> pendingDroppedMappings = new HashMap<>();

		public void addPendingDrop(Entry<?> entry, EntryMapping mapping) {
			this.pendingDroppedMappings.put(entry, mapping.targetName() != null ? mapping.targetName() : entry.getName());
		}

		public void applyPendingDrops(EntryTree<EntryMapping> mappings) {
			this.droppedMappings.putAll(this.pendingDroppedMappings);

			for (Entry<?> entry : this.droppedMappings.keySet()) {
				EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
				if (node == null) {
					continue;
				}

				for (Entry<?> childEntry : node.getChildrenRecursively()) {
					mappings.remove(childEntry);
				}
			}

			this.pendingDroppedMappings.clear();
		}

		public Map<Entry<?>, String> getDroppedAndPending() {
			var map = new HashMap<Entry<?>, String>();
			map.putAll(this.droppedMappings);
			map.putAll(this.pendingDroppedMappings);
			return map;
		}

		public Map<Entry<?>, String> getDroppedMappings() {
			return this.droppedMappings;
		}

		public Map<Entry<?>, String> getPendingDroppedMappings() {
			return this.pendingDroppedMappings;
		}
	}
}

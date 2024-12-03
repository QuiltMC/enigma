package org.quiltmc.enigma.impl.translation.mapping;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

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

	private Dropped dropMappings(ProgressListener progress, BiConsumer<Dropped, Entry<?>> dropper) {
		Dropped dropped = new Dropped();

		// HashEntryTree#getAllEntries filters out empty classes
		List<? extends Entry<?>> entries = StreamSupport.stream(this.mappings.spliterator(), false).map(EntryTreeNode::getEntry).toList();

		progress.init(entries.size(), "Checking for dropped mappings");

		int steps = 0;
		for (Entry<?> entry : entries) {
			progress.step(steps++, entry.toString());
			dropper.accept(dropped, entry);
		}

		dropped.apply(this.mappings);

		return dropped;
	}

	public Dropped dropBrokenMappings(ProgressListener progress) {
		return this.dropMappings(progress, this::tryDropBrokenEntry);
	}

	private void tryDropBrokenEntry(Dropped dropped, Entry<?> entry) {
		if (this.shouldDropBrokenEntry(dropped, entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropBrokenEntry(Dropped dropped, Entry<?> entry) {
		if (!this.index.getIndex(EntryIndex.class).hasEntry(entry)
				|| (entry instanceof LocalVariableEntry parameter && !this.project.validateParameterIndex(parameter))) {
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
		return !(entry instanceof MethodEntry) || this.hasNoChildren(entry, dropped);

		// Entry is not the root, and is not a method with params
	}

	public Dropped dropEmptyMappings(ProgressListener progress) {
		return this.dropMappings(progress, this::tryDropEmptyEntry);
	}

	private void tryDropEmptyEntry(Dropped dropped, Entry<?> entry) {
		if (this.shouldDropEmptyMapping(dropped, entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropEmptyMapping(Dropped dropped, Entry<?> entry) {
		EntryMapping mapping = this.mappings.get(entry);
		if (mapping != null) {
			boolean isEmpty = (mapping.targetName() == null && mapping.javadoc() == null) || !this.project.isRenamable(entry);

			if (isEmpty) {
				return this.hasNoChildren(entry, dropped);
			}
		}

		return false;
	}

	private boolean hasNoChildren(Entry<?> entry, Dropped dropped) {
		var children = this.mappings.getChildren(entry);

		// account for child mappings that have been dropped already
		if (!children.isEmpty()) {
			for (Entry<?> child : children) {
				var mapping = this.mappings.get(child);
				if (mapping != null && !(mapping.targetName() == null && mapping.javadoc() == null)) {
					return false;
				} else if (!dropped.getDroppedMappings().containsKey(child) && this.hasNoChildren(child, dropped)) {
					return true;
				}
			}
		}

		return children.isEmpty();
	}

	public static class Dropped {
		private final Map<Entry<?>, String> droppedMappings = new HashMap<>();

		public void drop(Entry<?> entry, EntryMapping mapping) {
			this.droppedMappings.put(entry, mapping.targetName() != null ? mapping.targetName() : entry.getName());
		}

		void apply(EntryTree<EntryMapping> mappings) {
			for (Entry<?> entry : this.droppedMappings.keySet()) {
				EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
				if (node == null) {
					continue;
				}

				for (Entry<?> childEntry : node.getChildrenRecursively()) {
					mappings.remove(childEntry);
				}
			}
		}

		public Map<Entry<?>, String> getDroppedMappings() {
			return this.droppedMappings;
		}
	}
}

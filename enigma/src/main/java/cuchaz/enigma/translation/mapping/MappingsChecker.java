/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MappingsChecker {
	private final JarIndex index;
	private final EntryTree<EntryMapping> mappings;

	public MappingsChecker(JarIndex index, EntryTree<EntryMapping> mappings) {
		this.index = index;
		this.mappings = mappings;
	}

	private Dropped dropMappings(ProgressListener progress, BiConsumer<Dropped, Entry<?>> dropper) {
		Dropped dropped = new Dropped();

		// HashEntryTree#getAllEntries filters out empty classes
		Stream<Entry<?>> allEntries = StreamSupport.stream(this.mappings.spliterator(), false).map(EntryTreeNode::getEntry);
		Collection<Entry<?>> obfEntries = allEntries
				.filter(e -> e instanceof ClassEntry || e instanceof MethodEntry || e instanceof FieldEntry || e instanceof LocalVariableEntry)
				.toList();

		progress.init(obfEntries.size(), "Checking for dropped mappings");

		int steps = 0;
		for (Entry<?> entry : obfEntries) {
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
		if (this.shouldDropBrokenEntry(entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropBrokenEntry(Entry<?> entry) {
		if (!this.index.getEntryIndex().hasEntry(entry)) {
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
		return !(entry instanceof MethodEntry) || this.mappings.getChildren(entry).isEmpty();

		// Entry is not the root, and is not a method with params
	}

	public Dropped dropEmptyMappings(ProgressListener progress) {
		return this.dropMappings(progress, this::tryDropEmptyEntry);
	}

	private void tryDropEmptyEntry(Dropped dropped, Entry<?> entry) {
		if (this.shouldDropEmptyMapping(entry)) {
			EntryMapping mapping = this.mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropEmptyMapping(Entry<?> entry) {
		EntryMapping mapping = this.mappings.get(entry);
		if (mapping != null) {
			boolean isEmpty = mapping.targetName() == null && mapping.javadoc() == null && mapping.accessModifier() == AccessModifier.UNCHANGED;
			if (isEmpty) {
				return this.mappings.getChildren(entry).isEmpty();
			}
		}

		return false;
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

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

public class MappingsChecker {
	private final JarIndex index;
	private final EntryTree<EntryMapping> mappings;

	public MappingsChecker(JarIndex index, EntryTree<EntryMapping> mappings) {
		this.index = index;
		this.mappings = mappings;
	}

	private Dropped dropMappings(ProgressListener progress, BiConsumer<Dropped, Entry<?>> dropper) {
		Dropped dropped = new Dropped();

		Collection<Entry<?>> obfEntries = mappings.getAllEntries()
				.filter(e -> e instanceof ClassEntry || e instanceof MethodEntry || e instanceof FieldEntry || e instanceof LocalVariableEntry)
				.toList();

		progress.init(obfEntries.size(), "Checking for dropped mappings");

		int steps = 0;
		for (Entry<?> entry : obfEntries) {
			progress.step(steps++, entry.toString());
			dropper.accept(dropped, entry);
		}

		dropped.apply(mappings);

		return dropped;
	}

	public Dropped dropBrokenMappings(ProgressListener progress) {
		return dropMappings(progress, this::tryDropBrokenEntry);
	}

	private void tryDropBrokenEntry(Dropped dropped, Entry<?> entry) {
		if (shouldDropBrokenEntry(entry)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropBrokenEntry(Entry<?> entry) {
		if (!index.getEntryIndex().hasEntry(entry)) {
			return true;
		}
		Collection<Entry<?>> resolvedEntries = index.getEntryResolver().resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT);
		return !resolvedEntries.contains(entry);
	}

	public Dropped dropEmptyMappings(ProgressListener progress) {
		System.out.println("Dropping empty mappings");
		return dropMappings(progress, this::tryDropEmptyEntry);
	}

	private void tryDropEmptyEntry(Dropped dropped, Entry<?> entry) {
		if (shouldDropEmptyMapping(entry)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropEmptyMapping(Entry<?> entry) {
		if (entry.toString().contains("C_kdrxhxjj")) {
			int x = 0;
			System.out.println("Hey");
		}
		EntryMapping mapping = mappings.get(entry);
		if (mapping != null) {
			if (mapping.targetName() == null) {
				return mappings.getChildren(entry).isEmpty();
			}
		}

		return false;
	}

	public static class Dropped {
		private final Map<Entry<?>, String> droppedMappings = new HashMap<>();

		public void drop(Entry<?> entry, EntryMapping mapping) {
			droppedMappings.put(entry, mapping.targetName() != null ? mapping.targetName() : entry.getName());
		}

		void apply(EntryTree<EntryMapping> mappings) {
			for (Entry<?> entry : droppedMappings.keySet()) {
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
			return droppedMappings;
		}
	}
}

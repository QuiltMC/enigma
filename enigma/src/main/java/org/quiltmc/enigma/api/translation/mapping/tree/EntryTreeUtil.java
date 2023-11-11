package org.quiltmc.enigma.api.translation.mapping.tree;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;

public class EntryTreeUtil {
	/**
	 * Inserts all entries from both trees into a merged {@link HashEntryTree}, without performing any modifications on the original trees.
	 * @return the merged tree
	 */
	public static EntryTree<EntryMapping> merge(EntryTree<EntryMapping> leftTree, EntryTree<EntryMapping> rightTree) {
		EntryTree<EntryMapping> merged = new HashEntryTree<>();

		rightTree.iterator().forEachRemaining(merged::insert);
		leftTree.iterator().forEachRemaining(node -> {
			if (merged.contains(node.getEntry())) {
				EntryMapping oldMapping = merged.get(node.getEntry());
				EntryMapping newMapping = node.getValue();

				if (oldMapping != null && newMapping != null) {
					EntryMapping mergedMapping = EntryMapping.merge(oldMapping, newMapping);
					merged.insert(node.getEntry(), mergedMapping);
				} else if (oldMapping == null && newMapping != null) {
					merged.insert(node);
				}
			} else {
				merged.insert(node);
			}
		});

		return merged;
	}
}

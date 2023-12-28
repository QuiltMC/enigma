package org.quiltmc.enigma.api.translation.mapping.tree;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A node of a {@link MergedEntryMappingTree}. Check the documentation of said class for details.
 */
public record MergedMappingTreeNode(EntryTreeNode<EntryMapping> mainNode, EntryTreeNode<EntryMapping> secondaryNode) implements EntryTreeNode<EntryMapping> {
	public MergedMappingTreeNode {
		if (!isMatchingNode(mainNode, secondaryNode)) {
			throw new IllegalArgumentException("The main and secondary nodes don't represent the same entry");
		}
	}

	public static boolean isMatchingNode(EntryTreeNode<EntryMapping> mainNode, EntryTreeNode<EntryMapping> secondaryNode) {
		return mainNode.getEntry().equals(secondaryNode.getEntry());
	}

	@Nullable
	@Override
	public EntryMapping getValue() {
		var main = this.mainNode.getValue();
		if (main == null) {
			return this.secondaryNode.getValue();
		}

		return main;
	}

	@Override
	public Entry<?> getEntry() {
		return this.mainNode.getEntry();
	}

	@Override
	public boolean isEmpty() {
		return this.mainNode.isEmpty() && this.secondaryNode.isEmpty();
	}

	@Override
	public Collection<Entry<?>> getChildren() {
		var children = new HashSet<>(this.mainNode.getChildren());
		children.addAll(this.secondaryNode.getChildren());
		return children;
	}

	@Override
	public Collection<? extends EntryTreeNode<EntryMapping>> getChildNodes() {
		Map<Entry<?>, EntryTreeNode<EntryMapping>> nodes = new HashMap<>();

		for (EntryTreeNode<EntryMapping> mainNode : this.mainNode.getChildNodes()) {
			nodes.put(mainNode.getEntry(), mainNode);
		}

		for (EntryTreeNode<EntryMapping> secondaryNode : this.secondaryNode.getChildNodes()) {
			nodes.merge(secondaryNode.getEntry(), secondaryNode, MergedMappingTreeNode::new);
		}

		return nodes.values();
	}
}

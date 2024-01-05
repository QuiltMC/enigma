package org.quiltmc.enigma.api.translation.mapping.tree;

import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link EntryMapping entry mapping} {@link EntryTree tree} that represents both a main and a secondary tree.
 * The secondary tree is used to provide entries that aren't present in the main one. Both trees are used by reference,
 * which means changes made directly to either of them, will also be reflected in the merged tree.
 *
 * <p>
 * Writing to the merged tree will only apply the changes to the main tree, while reading from it will include values
 * from both trees.
 */
public record MergedEntryMappingTree(EntryTree<EntryMapping> mainTree, EntryTree<EntryMapping> secondaryTree) implements EntryTree<EntryMapping> {
	@Override
	public void insert(Entry<?> entry, EntryMapping value) {
		this.mainTree.insert(entry, value);
	}

	@Nullable
	@Override
	public EntryMapping remove(Entry<?> entry) {
		return this.mainTree.remove(entry);
	}

	@Nullable
	@Override
	public EntryMapping get(Entry<?> entry) {
		EntryMapping main = this.mainTree.get(entry);
		if (main == null || (main.equals(EntryMapping.DEFAULT) && this.secondaryTree.contains(entry))) {
			return this.secondaryTree.get(entry);
		}

		return main;
	}

	@Override
	public boolean contains(Entry<?> entry) {
		return this.mainTree.contains(entry) || this.secondaryTree.contains(entry);
	}

	@Override
	public Collection<Entry<?>> getChildren(Entry<?> entry) {
		var leaf = this.findNode(entry);
		if (leaf == null) {
			return Collections.emptyList();
		}

		return leaf.getChildren();
	}

	@Override
	public Collection<Entry<?>> getSiblings(Entry<?> entry) {
		Entry<?> parent = entry.getParent();
		if (parent == null) {
			return getSiblings(entry, this.getRootNodes().map(EntryTreeNode::getEntry).collect(Collectors.toSet()));
		}

		return getSiblings(entry, this.getChildren(parent));
	}

	private static Collection<Entry<?>> getSiblings(Entry<?> entry, Collection<Entry<?>> generation) {
		var siblings = new HashSet<>(generation);
		siblings.remove(entry);
		return siblings;
	}

	@Override
	public EntryTreeNode<EntryMapping> findNode(Entry<?> entry) {
		var main = this.mainTree.findNode(entry);
		var secondary = this.secondaryTree.findNode(entry);

		if (main != null && secondary != null) {
			return new MergedMappingTreeNode(main, secondary);
		} else if (main == null) {
			return secondary;
		}

		return main;
	}

	static Stream<EntryTreeNode<EntryMapping>> mergeNodeStreams(Stream<EntryTreeNode<EntryMapping>> mainNodes, Stream<EntryTreeNode<EntryMapping>> secondaryNodes) {
		Map<Entry<?>, EntryTreeNode<EntryMapping>> nodes = new HashMap<>();

		mainNodes.forEach(mainNode -> nodes.put(mainNode.getEntry(), mainNode));
		secondaryNodes.forEach(secondaryNode -> nodes.merge(secondaryNode.getEntry(), secondaryNode, MergedMappingTreeNode::new));

		return nodes.values().stream();
	}

	@Override
	public Stream<EntryTreeNode<EntryMapping>> getRootNodes() {
		return mergeNodeStreams(this.mainTree.getRootNodes(), this.secondaryTree.getRootNodes());
	}

	@Nonnull
	@Override
	public Iterator<EntryTreeNode<EntryMapping>> iterator() {
		return this.getRootNodes().flatMap(n -> n.getNodesRecursively().stream())
			.map(n -> (EntryTreeNode<EntryMapping>) n) // ? extends EntryTreeNode<EntryMapping> -> EntryTreeNode<EntryMapping>
			.iterator();
	}

	@Override
	public Stream<Entry<?>> getAllEntries() {
		return this.getRootNodes().flatMap(n -> n.getChildrenRecursively().stream());
	}

	@Override
	public boolean isEmpty() {
		return this.mainTree.isEmpty() && this.secondaryTree.isEmpty();
	}

	@Override
	public MergedEntryMappingTree translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		var main = this.mainTree.translate(translator, resolver, mappings);
		var secondary = this.secondaryTree.translate(translator, resolver, mappings);
		return new MergedEntryMappingTree(main, secondary);
	}
}

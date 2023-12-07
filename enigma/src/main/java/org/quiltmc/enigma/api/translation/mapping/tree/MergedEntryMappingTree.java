package org.quiltmc.enigma.api.translation.mapping.tree;

import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.Pair;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * An {@link EntryMapping entry mapping} {@link EntryTree tree} that represents both a main and a secondary tree.
 * The secondary tree is used for entries that aren't contained in the main one, and methods to alter or retrieve values
 * from one or both of them are also provided.
 * <p>
 * Removing an entry by default removes from both the main and secondary trees.
 */
// TODO: make non abstract
public abstract class MergedEntryMappingTree implements EntryTree<EntryMapping> {
	private final EntryTree<EntryMapping> mainTree;
	private final EntryTree<EntryMapping> secondaryTree;

	public MergedEntryMappingTree(EntryTree<EntryMapping> mainTree, EntryTree<EntryMapping> secondaryTree) {
		this.mainTree = mainTree;
		this.secondaryTree = secondaryTree;
	}

	@Override
	public void insert(Entry<?> entry, EntryMapping value) {
		this.mainTree.insert(entry, value);
		if (value == null) {
			this.insertSecondary(entry, null);
		}
	}

	public void insertBoth(Entry<?> entry, EntryMapping value) {
		this.insert(entry, value);
		if (value != null) {
			this.insertSecondary(entry, value);
		}
	}

	public void insertMain(Entry<?> entry, EntryMapping value) {
		this.insert(entry, value);
	}

	public void insertSecondary(Entry<?> entry, EntryMapping value) {
		this.secondaryTree.insert(entry, value);
	}

	@Nullable
	@Override
	public EntryMapping remove(Entry<?> entry) {
		EntryMapping main = this.removeMain(entry);
		this.removeSecondary(entry);
		return main;
	}

	public Pair<EntryMapping, EntryMapping> removeBoth(Entry<?> entry) {
		return new Pair<>(this.removeMain(entry), this.removeSecondary(entry));
	}

	public EntryMapping removeMain(Entry<?> entry) {
		return this.mainTree.remove(entry);
	}

	public EntryMapping removeSecondary(Entry<?> entry) {
		return this.secondaryTree.remove(entry);
	}

	@Nullable
	@Override
	public EntryMapping get(Entry<?> entry) {
		EntryMapping main = this.getMain(entry);
		if (main == null) {
			return this.getSecondary(entry);
		}

		return main;
	}

	public Pair<EntryMapping, EntryMapping> getBoth(Entry<?> entry) {
		return new Pair<>(this.getMain(entry), this.getSecondary(entry));
	}


	public EntryMapping getMain(Entry<?> entry) {
		return this.mainTree.get(entry);
	}

	public EntryMapping getSecondary(Entry<?> entry) {
		return this.secondaryTree.get(entry);
	}

	@Override
	public boolean contains(Entry<?> entry) {
		return this.mainContains(entry) || this.secondaryContains(entry);
	}

	public boolean bothContain(Entry<?> entry) {
		return this.mainContains(entry) && this.secondaryContains(entry);
	}

	public boolean mainContains(Entry<?> entry) {
		return this.mainTree.contains(entry);
	}

	public boolean secondaryContains(Entry<?> entry) {
		return this.secondaryTree.contains(entry);
	}

	@Override
	public Collection<Entry<?>> getChildren(Entry<?> entry) {
		var main = this.getMainChildren(entry);
		if (main == null || main.isEmpty()) {
			return this.getSecondaryChildren(entry);
		}

		return main;
	}

	public Pair<Collection<Entry<?>>, Collection<Entry<?>>> getBothChildren(Entry<?> entry) {
		return new Pair<>(this.getMainChildren(entry), this.getSecondaryChildren(entry));
	}

	public Collection<Entry<?>> getMainChildren(Entry<?> entry) {
		return this.mainTree.getChildren(entry);
	}

	public Collection<Entry<?>> getSecondaryChildren(Entry<?> entry) {
		return this.secondaryTree.getChildren(entry);
	}

	@Override
	public Collection<Entry<?>> getSiblings(Entry<?> entry) {
		var main = this.getMainSiblings(entry);
		if (main == null || main.isEmpty()) {
			return this.getSecondarySiblings(entry);
		}

		return main;
	}

	public Pair<Collection<Entry<?>>, Collection<Entry<?>>> getBothSiblings(Entry<?> entry) {
		return new Pair<>(this.getMainSiblings(entry), this.getSecondarySiblings(entry));
	}

	public Collection<Entry<?>> getMainSiblings(Entry<?> entry) {
		return this.mainTree.getSiblings(entry);
	}

	public Collection<Entry<?>> getSecondarySiblings(Entry<?> entry) {
		return this.secondaryTree.getSiblings(entry);
	}

	@Override
	public EntryTreeNode<EntryMapping> findNode(Entry<?> entry) {
		var main = this.findMainNode(entry);
		if (main == null || main.isEmpty()) {
			return this.findSecondaryNode(entry);
		}

		return main;
	}

	public Pair<EntryTreeNode<EntryMapping>, EntryTreeNode<EntryMapping>> findBothNodes(Entry<?> entry) {
		return new Pair<>(this.findMainNode(entry), this.findSecondaryNode(entry));
	}

	public EntryTreeNode<EntryMapping> findMainNode(Entry<?> entry) {
		return this.mainTree.findNode(entry);
	}

	public EntryTreeNode<EntryMapping> findSecondaryNode(Entry<?> entry) {
		return this.secondaryTree.findNode(entry);
	}

	// TODO: iterator()

	// TODO: getAllEntries()

	// TODO: getRootNodes()

	@Override
	public boolean isEmpty() {
		return this.isMainEmpty() && this.isSecondaryEmpty();
	}

	public boolean isAnyTreeEmpty() {
		return this.isMainEmpty() || this.isSecondaryEmpty();
	}

	private boolean isMainEmpty() {
		return this.mainTree.isEmpty();
	}

	private boolean isSecondaryEmpty() {
		return this.secondaryTree.isEmpty();
	}

	// TODO
	// @Override
	// public MergedEntryMappingTree translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
	// 	var main = this.translateMain(translator, resolver, mappings);
	// 	var secondary = this.translateSecondary(translator, resolver, mappings);
	// 	return new MergedEntryMappingTree(main, secondary);
	// }

	public EntryTree<EntryMapping> translateMain(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return this.mainTree.translate(translator, resolver, mappings);
	}

	public EntryTree<EntryMapping> translateSecondary(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return this.secondaryTree.translate(translator, resolver, mappings);
	}
}

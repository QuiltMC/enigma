package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;

/**
 * A live, unmodifiable view of a {@link MutableMultiTrie},
 * for use in {@link MutableMultiTrie#getView()} implementations.
 */
public final class View<K, V> implements MultiTrie<K, V> {
	private final MutableMultiTrie<K, V, ? extends MutableMultiTrie.Node<K, V>> viewed;

	public View(MutableMultiTrie<K, V, ? extends MutableMultiTrie.Node<K, V>> viewed) {
		this.viewed = Utils.requireNonNull(viewed, "viewed");
	}

	@Nonnull
	@Override
	public Node<K, V> getRoot() {
		return this.viewed.getRoot().getView();
	}
}

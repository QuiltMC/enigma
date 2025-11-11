package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;

/**
 * A live, unmodifiable view of a {@link MutableMultiTrie},
 * for use in {@link MutableMultiTrie#getView()} implementations.
 */
public final class View<K, S, V> implements MultiTrie<K, S, V> {
	private final MutableMultiTrie<K, S, V> viewed;

	public View(MutableMultiTrie<K, S, V> viewed) {
		this.viewed = Utils.requireNonNull(viewed, "viewed");
	}

	@Nonnull
	@Override
	public Node<K, V> getRoot() {
		return this.viewed.getRoot();
	}

	@Nonnull
	@Override
	public Node<K, V> get(S prefix) {
		return this.viewed.get(prefix);
	}
}

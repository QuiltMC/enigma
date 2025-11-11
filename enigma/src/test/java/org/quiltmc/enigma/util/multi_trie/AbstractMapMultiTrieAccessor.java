package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;

import javax.annotation.Nonnull;

public class AbstractMapMultiTrieAccessor<K, S, V, N extends AbstractMapMultiTrie.Node<K, V, N>>
		extends AbstractMapMultiTrie<K, S, V, N> {
	public static <K, S, V> BiMap<K, ? extends MultiTrie.Node<K, V>> getRootChildren(
			AbstractMapMultiTrie<K, S, V, ?> trie
	) {
		final AbstractMapMultiTrieAccessor<K, S, V, ?> accessor = new AbstractMapMultiTrieAccessor<>(trie);
		return accessor.root.children;
	}

	public AbstractMapMultiTrieAccessor(AbstractMapMultiTrie<K, S, V, N> accessed) {
		super(accessed.root);
	}

	@Nonnull
	@Override
	public MultiTrie.Node<K, V> get(S prefix) {
		throw new UnsupportedOperationException();
	}
}

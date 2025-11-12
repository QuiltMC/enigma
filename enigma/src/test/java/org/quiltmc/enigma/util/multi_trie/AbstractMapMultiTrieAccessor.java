package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;

public class AbstractMapMultiTrieAccessor<K, V, N extends AbstractMapMultiTrie.Node<K, V, N>>
		extends AbstractMapMultiTrie<K, V, N> {
	public static <K, V> BiMap<K, ? extends MultiTrie.Node<K, V>> getRootChildren(
			AbstractMapMultiTrie<K, V, ? extends MultiTrie.Node<K, V>> trie
	) {
		return new AbstractMapMultiTrieAccessor<>(trie).root.children;
	}

	public AbstractMapMultiTrieAccessor(AbstractMapMultiTrie<K, V, N> accessed) {
		super(accessed.root);
	}
}

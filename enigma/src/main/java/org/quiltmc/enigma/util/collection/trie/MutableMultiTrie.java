package org.quiltmc.enigma.util.collection.trie;

public interface MutableMultiTrie<K, S, V> extends MultiTrie<K, S, V> {
	void put(S sequence, V value);

	boolean remove(S sequence, V value);

	boolean removeAll(S sequence);

	MultiTrie<K, S, V> view();

	interface Node<K, S, V> extends MultiTrie.Node<K, V> {
		void put(S sequence, V value);

		boolean remove(S sequence, V value);

		boolean removeAll(S sequence);
	}
}

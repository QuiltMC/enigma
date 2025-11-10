package org.quiltmc.enigma.util.collection.trie;

public interface MutableMultiTrie<K, S, V> extends MultiTrie<K, S, V> {
	void put(S sequence, V value);

	boolean remove(S sequence, V value);

	boolean removeAll(S sequence);

	/**
	 * @return a live, unmodifiable view of this trie
	 */
	MultiTrie<K, S, V> view();

	/**
	 * @implSpec implementations should not have {@code public} visibility; users should never see node mutation methods
	 */
	interface Node<K, S, V> extends MultiTrie.Node<K, V> {
		void put(S sequence, V value);

		boolean remove(S sequence, V value);

		boolean removeAll(S sequence);
	}
}

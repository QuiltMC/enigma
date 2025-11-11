package org.quiltmc.enigma.util.multi_trie;

/**
 * A multi-trie that allows modification which can also provide unmodifiable views of its contents.
 *
 * @param <K> the type of keys
 * @param <S> the type of sequences
 * @param <V> the type of values
 */
public interface MutableMultiTrie<K, S, V> extends MultiTrie<K, S, V> {
	void put(S sequence, V value);

	boolean remove(S sequence, V value);

	boolean removeAll(S sequence);

	/**
	 * @return a live, unmodifiable view of this trie
	 */
	MultiTrie<K, S, V> getView();

	/**
	 * @implSpec mutable nodes should not be returned from public methods, return a
	 * {@linkplain #getView() view} instead; users should never see node mutation methods
	 *
	 * @implNote most implementations should remove themselves from any
	 * backing data structures when the node becomes empty
	 */
	interface Node<K, S, V> extends MultiTrie.Node<K, V> {
		void put(S sequence, V value);

		boolean remove(S sequence, V value);

		boolean removeAll(S sequence);

		/**
		 * @return al live, unmodifiable view of this node
		 */
		MultiTrie.Node<K, V> getView();
	}
}

package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.multi_trie.MutableMultiTrie.Node;

import javax.annotation.Nonnull;

/**
 * A multi-trie that allows modification which can also provide unmodifiable views of its contents.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface MutableMultiTrie<K, V, N extends Node<K, V>> extends MultiTrie<K, V> {
	@Nonnull
	@Override
	N getRoot();

	/**
	 * @return a live, unmodifiable view of this trie
	 */
	MultiTrie<K, V> getView();

	/**
	 * A mutable node representing values associated with a {@link MutableMultiTrie}.
	 *
	 * @implNote most implementations should remove themselves from any
	 * backing data structures when the node becomes empty
	 */
	interface Node<K, V> extends MultiTrie.Node<K, V> {
		@Override
		@Nonnull
		Node<K, V> next(K key);

		/**
		 * @param value a value to add to this node's leaves, associating it with the sequence leading to this node.
		 */
		void put(V value);

		/**
		 * @param value a value to remove from this node's leaves
		 *
		 * @return {@code true} if a value was removed, or {@code false} otherwise
		 */
		boolean remove(V value);

		/**
		 * Removes all leaves from this node.
		 *
		 * @return {@code true} if any values were removed, or {@code false} otherwise
		 */
		boolean removeAll();

		/**
		 * @return a live, unmodifiable view of this node
		 */
		MultiTrie.Node<K, V> getView();
	}
}

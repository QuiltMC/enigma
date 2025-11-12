package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.multi_trie.MutableMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
		@Nullable
		Node<K, V> next(K key);

		/**
         * @param value      the value to associate
         * @param sequence   the full sequence the value will be associated with in this node's trie
         * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 */
		void put(V value);

		/**
		 * @param value      the value to dissociate
		 * @param sequence   the full sequence the value may be associated with in this node's trie
		 * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 *
		 * @return {@code true} if a value was dissociated, or {@code false} otherwise
		 */
		boolean remove(V value);

		/**
		 * @param sequence   the full sequence whose values are to be dissociated
		 * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 *
		 * @return {@code true} if any values were dissociated, or {@code false} otherwise
		 */
		boolean removeAll();

		/**
		 * @return a live, unmodifiable view of this node
		 */
		MultiTrie.Node<K, V> getView();
	}
}

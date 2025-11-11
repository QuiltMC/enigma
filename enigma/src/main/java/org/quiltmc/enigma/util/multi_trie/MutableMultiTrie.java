package org.quiltmc.enigma.util.multi_trie;

import org.checkerframework.dataflow.qual.Pure;

/**
 * A multi-trie that allows modification which can also provide unmodifiable views of its contents.
 *
 * @param <K> the type of keys
 * @param <S> the type of sequences; should generally support constant-time random key access for fast
 *            {@link Node#getKey(Object, int)} implementations
 * @param <V> the type of values
 */
public interface MutableMultiTrie<K, S, V> extends MultiTrie<K, S, V> {
	/**
	 * Associates the passed {@code value} with the passed {@code sequence} of keys.
	 */
	void put(S sequence, V value);

	/**
	 * Removes any association between the passed {@code sequence} of keys and the passed {@code value}.
	 *
	 * <p> <em>Note:</em> this removes {@linkplain Node#streamLeaves() leaves},
	 * not {@linkplain Node#streamBranches() branches}
	 *
	 * @return {@code true} if the value was dissociated, or {@code false} otherwise
	 */
	boolean remove(S sequence, V value);

	/**
	 * Removes all associations with the passed {@code sequence} of keys.
	 *
	 * <p> <em>Note:</em> this removes {@linkplain Node#streamLeaves() leaves},
	 * not {@linkplain Node#streamBranches() branches}
	 *
	 * @return {@code true} if any values were dissociated, or {@code false} otherwise
	 */
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
		/**
		 * @param value      the value to associate
		 * @param sequence   the full sequence the value will be associated with in this node's trie
		 * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 */
		void put(S sequence, V value, int startIndex);

		/**
		 * @param value      the value to dissociate
		 * @param sequence   the full sequence the value may be associated with in this node's trie
		 * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 *
		 * @return {@code true} if a value was dissociated, or {@code false} otherwise
		 */
		boolean remove(S sequence, V value, int startIndex);

		/**
		 * @param sequence   the full sequence whose values are to be dissociated
		 * @param startIndex the index in the passed {@code sequence} at which this node's suffix starts;
		 *                   may be greater than or equal to the passed {@code sequence}'s
		 *                   {@linkplain #getLength(Object) length}, indicating that the value is a leaf
		 *
		 * @return {@code true} if any values were dissociated, or {@code false} otherwise
		 */
		boolean removeAll(S sequence, int startIndex);

		/**
		 * @return a live, unmodifiable view of this node
		 */
		MultiTrie.Node<K, V> getView();

		/**
		 * @return the number of keys in the passed {@code sequence}
		 */
		@Pure
		int getLength(S sequence);

		/**
		 * @return the key at the passed {@code index} within the passed {@code sequence}
		 *
		 * @implSpec should only be passed valid {@code index}es; i.e. non-negative {@code index}es which are less than
		 * the {@linkplain #getLength(Object) length} of the passed {@code sequence}
		 */
		@Pure
		K getKey(S sequence, int index);
	}
}

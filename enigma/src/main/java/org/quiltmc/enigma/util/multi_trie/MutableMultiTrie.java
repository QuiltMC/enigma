package org.quiltmc.enigma.util.multi_trie;

import java.util.stream.Stream;

/**
 * A multi-trie that allows modification which can also provide unmodifiable views of its contents.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface MutableMultiTrie<K, V> extends MultiTrie<K, V> {
	@Override
	MutableMultiTrie.Node<K, V> getRoot();

	/**
	 * @return a live, unmodifiable view of this trie
	 */
	MultiTrie<K, V> view();

	/**
	 * A mutable node representing values associated with a {@link MutableMultiTrie}.
	 *
	 * @implNote most implementations should remove themselves from any
	 * backing data structures when the node becomes empty
	 */
	interface Node<K, V> extends MultiTrie.Node<K, V> {
		@Override
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
		boolean removeLeaf(V value);

		/**
		 * Removes all leaves from this node.
		 *
		 * @return {@code true} if any values were removed, or {@code false} otherwise
		 */
		boolean clearLeaves();

		/**
		 * @return a live, unmodifiable view of this node
		 */
		MultiTrie.Node<K, V> view();

		abstract class View<K, V> implements MultiTrie.Node<K, V> {
			@Override
			public Stream<V> streamLeaves() {
				return this.getViewed().streamLeaves();
			}

			@Override
			public Stream<V> streamStems() {
				return this.getViewed().streamStems();
			}

			@Override
			public Stream<V> streamValues() {
				return this.getViewed().streamValues();
			}

			protected abstract Node<K, V> getViewed();
		}
	}
}

package org.quiltmc.enigma.util.multi_trie;

import com.google.common.base.Preconditions;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A multi-trie (or prefix tree) associates a sequence of keys with one or more values.
 *
 * <p> Values can be looked up by a prefix of their key sequence; a {@link Node} holding all values associated with a
 * sequence beginning with the prefix will be returned.<br>
 * The prefix is passed key-by-key to {@link Node#next} starting with {@link #getRoot}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface MultiTrie<K, V> {
	/**
	 * The root is the node associated with the empty sequence.
	 *
	 * <p> Other nodes can be looked up via the root.
	 */
	Node<K, V> getRoot();

	/**
	 * @return the total number of values in this trie
	 */
	default long getSize() {
		return this.getRoot().getSize();
	}

	/**
	 * @return {@code true} if this trie contains no values, or {@code false} otherwise
	 */
	default boolean isEmpty() {
		return this.getSize() == 0;
	}

	/**
	 * Represents values associated with a prefix in a {@link MultiTrie}.
	 *
	 * @param <K> the type of keys
	 * @param <V> the type of values
	 */
	interface Node<K, V> {
		static <K, V, N extends Node<K, V>> N previous(N node, int steps, UnaryOperator<N> previous) {
			Preconditions.checkArgument(steps >= 0, "steps must not be negative!");

			if (steps == 0) {
				return node;
			}

			N prev = previous.apply(node);
			while (--steps > 0 && prev.getDepth() > 0) {
				prev = previous.apply(prev);
			}

			return prev;
		}

		/**
		 * @return a {@link Stream} containing all values with no more keys in their associated sequence;<br>
		 * i.e. the prefix this node is associated with is the <em>whole</em> sequence the values are associated with
		 */
		Stream<V> streamLeaves();

		/**
		 * @return a {@link Stream} containing all values with more keys in their associated sequence;<br>
		 * i.e. the prefix this node is associated with is <em>not</em>
		 * the whole sequence the values are associated with
		 */
		Stream<V> streamStems();

		/**
		 * @return a {@link Stream} containing all values associated with the prefix this node is associated with
		 */
		default Stream<V> streamValues() {
			return Stream.concat(this.streamLeaves(), this.streamStems());
		}

		/**
		 * @return the total number of {@linkplain #streamValues() values} associated with this node's prefix
		 */
		default long getSize() {
			return this.streamValues().count();
		}

		/**
		 * @return {@code true} if this node contains no {@linkplain #streamValues() values}, or {@code false} otherwise
		 *
		 * @see #isNonEmpty()
		 */
		default boolean isEmpty() {
			return !this.isNonEmpty();
		}

		/**
		 * @return {@code false} if this node contains no {@linkplain #streamValues()}, or {@code true} otherwise
		 *
		 * @see #isEmpty()
		 */
		default boolean isNonEmpty() {
			return this.getSize() > 0;
		}

		/**
		 * @return the node associated with the sequence formed by appending the passed
		 * {@code key} to this node's sequence
		 */
		Node<K, V> next(K key);

		/**
		 * @return this node's parent if it is not the {@linkplain #getRoot() root}, otherwise returns this node
		 */
		Node<K, V> previous();

		/**
		 * Equivalent to chaining {@link #previous()} calls a number of times equal to the passed {@code steps}.
		 *
		 * @throws IllegalArgumentException if the passed {@code steps} is negative
		 */
		Node<K, V> previous(int steps);

		/**
		 * @return this node's depth: the length of the prefix it is associated with; the root returns {@code 0},
		 * non-roots return positive integers
		 */
		int getDepth();
	}
}

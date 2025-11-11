package org.quiltmc.enigma.util.multi_trie;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * A multi-trie (or prefix tree) associates a sequence of keys with one or more values.
 *
 * <p> Values can be looked up by a prefix of their key sequence; all values associated with a sequence beginning with
 * the prefix will be returned.<br>
 * The prefix can be passed either all at once to {@link #get},
 * or key-by-key to {@link Node#next} starting with {@link #getRoot}.
 *
 * @implSpec {@code S} sequence types should represent an ordered sequence of keys of type {@code K};
 * sequences that represent the same sequence of keys should be equivalent
 *
 * @param <K> the type of keys
 * @param <S> the type of sequences
 * @param <V> the type of values
 */
public interface MultiTrie<K, S, V> {
	@Nonnull
	Node<K, V> getRoot();

	@Nonnull
	Node<K, V> get(S prefix);

	default long getSize() {
		return this.getRoot().getSize();
	}

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
		Stream<V> streamBranches();

		/**
		 * @return a {@link Stream} containing all values associated with the prefix this node is associated with
		 */
		Stream<V> streamValues();

		default long getSize() {
			return this.streamValues().count();
		}

		default boolean isEmpty() {
			return this.getSize() == 0;
		}

		@Nonnull
		Node<K, V> next(K key);
	}
}

package org.quiltmc.enigma.util.multi_trie;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * A multi-trie (or prefix tree) associates a sequence of keys with one or more values.
 *
 * <p> Values can be looked up by a prefix of their key sequence; all values associated with a sequence beginning with
 * the prefix will be returned.
 *
 * @param <K> the type of keys
 * @param <S> the type of sequences
 * @param <V> the type of values
 */
public interface MultiTrie<K, S, V> {
	Node<K, V> getRoot();

	default Node<K, V> start(K key) {
		return this.getRoot().next(key);
	}

	@Nonnull
	Node<K, V> get(S prefix);

	default long getSize() {
		return this.getRoot().getSize();
	}

	default boolean isEmpty() {
		return this.getSize() == 0;
	}

	interface Node<K, V> {
		Stream<V> streamLeaves();
		Stream<V> streamBranches();
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

package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A {@link MultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds {@link String}/{@link Character}-specific access methods:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #streamIgnoreCase(String)}
 *     <li> {@link Node#streamNextIgnoreCase(Character)}
 * </ul>
 *
 * @param <V> the type of values
 */
public interface StringMultiTrie<V> extends MultiTrie<Character, V> {
	static Optional<Character> tryToggleCase(char c) {
		if (Character.isUpperCase(c)) {
			return Optional.of(Character.toLowerCase(c));
		} else if (Character.isLowerCase(c)) {
			return Optional.of(Character.toUpperCase(c));
		} else {
			return Optional.empty();
		}
	}

	static <V, N extends Node<V>> N get(String prefix, N root, BiFunction<N, Character, N> next) {
		Utils.requireNonNull(prefix, "prefix");

		N node = root;
		for (int i = 0; i < prefix.length(); i++) {
			node = next.apply(node, prefix.charAt(i));
		}

		return node;
	}

	@Override
	Node<V> getRoot();

	Node<V> get(String prefix);

	Stream<Node<V>> streamIgnoreCase(String prefix);

	interface Node<V> extends MultiTrie.Node<Character, V> {
		@Override
		Node<V> next(Character key);

		Stream<Node<V>> streamNextIgnoreCase(Character key);

		@Override
		Node<V> previous();

		@Override
		Node<V> previous(int steps);
	}
}

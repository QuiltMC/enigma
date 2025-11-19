package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.function.BiFunction;

/**
 * A {@link MultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds {@link String}/{@link Character}-specific access methods:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #getIgnoreCase(String)}
 *     <li> {@link Node#nextIgnoreCase(Character)}
 * </ul>
 *
 * @param <V> the type of values
 */
public interface StringMultiTrie<V> extends MultiTrie<Character, V> {
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

	Node<V> getIgnoreCase(String prefix);

	interface Node<V> extends MultiTrie.Node<Character, V> {
		@Override
		Node<V> next(Character key);

		default Node<V> nextIgnoreCase(Character key) {
			final Node<V> next = this.next(key);
			if (next.isEmpty()) {
				final char c = key;
				if (Character.isUpperCase(c)) {
					return this.next(Character.toLowerCase(c));
				} else if (Character.isLowerCase(c)) {
					return this.next(Character.toUpperCase(c));
				} else {
					return next;
				}
			} else {
				return next;
			}
		}

		@Override
		Node<V> previous();

		@Override
		Node<V> previous(int steps);
	}
}

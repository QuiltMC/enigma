package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link MultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds {@link String}/{@link Character}-specific convenience methods for accessing its contents:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #getIgnoreCase(String)}
 *     <li> {@link CharacterNode#nextIgnoreCase(Character)}
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

	static <V, N extends CharacterNode<V>> N get(String prefix, N root, BiFunction<N, Character, N> next) {
		Utils.requireNonNull(prefix, "prefix");

		N node = root;
		for (int i = 0; i < prefix.length(); i++) {
			node = next.apply(node, prefix.charAt(i));
		}

		return node;
	}

	@Override
	CharacterNode<V> getRoot();

	CharacterNode<V> get(String prefix);

	CharacterNode<V> getIgnoreCase(String prefix);

	interface CharacterNode<V> extends MultiTrie.Node<Character, V> {
		@Override
		CharacterNode<V> next(Character key);

		default CharacterNode<V> nextIgnoreCase(Character key) {
			final CharacterNode<V> next = this.next(key);
			return next.isEmpty()
					? tryToggleCase(key).map(this::next).orElse(next)
					: next;
		}

		@Override
		CharacterNode<V> previous();

		@Override
		CharacterNode<V> previous(int steps);
	}
}

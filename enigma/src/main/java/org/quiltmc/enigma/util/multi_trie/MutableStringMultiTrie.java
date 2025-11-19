package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.Optional;

/**
 * A {@link MutableMultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds {@link String}/{@link Character}-specific convenience methods for accessing its contents:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #getIgnoreCase(String)}
 *     <li> {@link #put(String, Object)}
 *     <li> {@link #remove(String, Object)}
 *     <li> {@link #removeAll(String)}
 *     <li> {@link CharacterNode#nextIgnoreCase(Character)}
 * </ul>
 *
 * <p> {@linkplain #view() Views} also provide {@link View.AbstractView#get(String) get} and
 * {@link View.AbstractView#getIgnoreCase(String) getIgnoreCase} methods.
 *
 * @param <V> the type of values
 */
public interface MutableStringMultiTrie<V> extends MutableMultiTrie<Character, V>, StringMultiTrie<V> {
	static Optional<Character> tryToggleCase(char c) {
		if (Character.isUpperCase(c)) {
			return Optional.of(Character.toLowerCase(c));
		} else if (Character.isLowerCase(c)) {
			return Optional.of(Character.toUpperCase(c));
		} else {
			return Optional.empty();
		}
	}

	String STRING = "string";
	String VALUE = "value";

	@Override
	MutableCharacterNode<V> getRoot();

	@Override
	default MutableCharacterNode<V> get(String prefix) {
		return StringMultiTrie.get(prefix, this.getRoot(), MutableCharacterNode::next);
	}

	@Override
	default MutableCharacterNode<V> getIgnoreCase(String prefix) {
		return StringMultiTrie.get(prefix, this.getRoot(), MutableCharacterNode::nextIgnoreCase);
	}

	@Override
	View<V> view();

	default MutableCharacterNode<V> put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		MutableCharacterNode<V> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));
		}

		node.put(value);

		return node;
	}

	default boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		MutableCharacterNode<V> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));

			if (node.isEmpty()) {
				return false;
			}
		}

		return node.removeLeaf(value);
	}

	default boolean removeAll(String string) {
		Utils.requireNonNull(string, STRING);

		MutableCharacterNode<V> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));

			if (node.isEmpty()) {
				return false;
			}
		}

		return node.clearLeaves();
	}

	interface MutableCharacterNode<V> extends CharacterNode<V>, MutableMultiTrie.Node<Character, V> {
		@Override
		MutableCharacterNode<V> next(Character key);

		@Override
		MutableCharacterNode<V> previous();

		@Override
		MutableCharacterNode<V> previous(int steps);

		@Override
		CharacterNode<V> view();

		@Override
		default MutableCharacterNode<V> nextIgnoreCase(Character key) {
			final MutableCharacterNode<V> next = this.next(key);
			return next.isEmpty()
					? tryToggleCase(key).map(this::next).orElse(next)
					: next;
		}
	}

	interface View<V> extends StringMultiTrie<V> {
		@Override
		CharacterNode<V> get(String prefix);

		@Override
		CharacterNode<V> getIgnoreCase(String prefix);

		abstract class AbstractView<V> implements View<V> {
			@Override
			public CharacterNode<V> getRoot() {
				return this.getViewed().getRoot().view();
			}

			@Override
			public CharacterNode<V> get(String prefix) {
				return this.getViewed().get(prefix).view();
			}

			@Override
			public CharacterNode<V> getIgnoreCase(String prefix) {
				return this.getViewed().getIgnoreCase(prefix).view();
			}

			protected abstract MutableStringMultiTrie<V> getViewed();
		}
	}
}

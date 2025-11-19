package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.Optional;

/**
 * A {@linkplain  MutableMultiTrie mutable} {@link StringMultiTrie}.
 *
 * <p> Adds {@link String}-specific mutation methods:
 * <ul>
 *     <li> {@link #put(String, Object)}
 *     <li> {@link #remove(String, Object)}
 *     <li> {@link #removeAll(String)}
 * </ul>
 *
 * <p> {@linkplain #view() Views} are also {@link StringMultiTrie}s.
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
	Node<V> getRoot();

	@Override
	default Node<V> get(String prefix) {
		return StringMultiTrie.get(prefix, this.getRoot(), Node::next);
	}

	@Override
	default Node<V> getIgnoreCase(String prefix) {
		return StringMultiTrie.get(prefix, this.getRoot(), Node::nextIgnoreCase);
	}

	@Override
	StringMultiTrie<V> view();

	default Node<V> put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		Node<V> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));
		}

		node.put(value);

		return node;
	}

	default boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		Node<V> node = this.getRoot();
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

		Node<V> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));

			if (node.isEmpty()) {
				return false;
			}
		}

		return node.clearLeaves();
	}

	interface Node<V> extends StringMultiTrie.Node<V>, MutableMultiTrie.Node<Character, V> {
		@Override
		Node<V> next(Character key);

		@Override
		Node<V> previous();

		@Override
		Node<V> previous(int steps);

		@Override
		StringMultiTrie.Node<V> view();

		@Override
		default Node<V> nextIgnoreCase(Character key) {
			final Node<V> next = this.next(key);
			return next.isEmpty()
					? tryToggleCase(key).map(this::next).orElse(next)
					: next;
		}
	}

	abstract class AbstractView<V> implements StringMultiTrie<V> {
		@Override
		public Node<V> getRoot() {
			return this.getViewed().getRoot().view();
		}

		@Override
		public Node<V> get(String prefix) {
			return this.getViewed().get(prefix).view();
		}

		@Override
		public Node<V> getIgnoreCase(String prefix) {
			return this.getViewed().getIgnoreCase(prefix).view();
		}

		protected abstract MutableStringMultiTrie<V> getViewed();
	}
}

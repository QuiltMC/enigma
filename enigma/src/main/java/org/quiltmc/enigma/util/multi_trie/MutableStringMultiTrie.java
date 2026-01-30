package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import java.util.List;
import java.util.stream.Stream;

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
	String STRING = "string";
	String VALUE = "value";

	@Override
	Node<V> getRoot();

	@Override
	default Node<V> get(String prefix) {
		return StringMultiTrie.get(prefix, this.getRoot(), Node::next);
	}

	@Override
	default Stream<StringMultiTrie.Node<V>> streamIgnoreCase(String prefix) {
		Utils.requireNonNull(prefix, "prefix");

		if (this.isEmpty()) {
			return Stream.empty();
		}

		List<StringMultiTrie.Node<V>> nodes = List.of(this.getRoot().view());
		for (int i = 0; i < prefix.length(); i++) {
			final Character key = prefix.charAt(i);
			nodes = nodes.stream()
					.flatMap(node -> node.streamNextIgnoreCase(key))
					.filter(StringMultiTrie.Node::isNonEmpty)
					.toList();
			if (nodes.isEmpty()) {
				return Stream.empty();
			}
		}

		return nodes.stream();
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
		default Stream<StringMultiTrie.Node<V>> streamNextIgnoreCase(Character key) {
			final Node<V> next = this.next(key);
			return Stream.concat(
				next.isEmpty() ? Stream.empty() : Stream.of(next.view()),
				StringMultiTrie.tryToggleCase(key)
					.map(this::next)
					.filter(Node::isNonEmpty)
					.map(Node::view)
					.stream()
			);
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
		public Stream<StringMultiTrie.Node<V>> streamIgnoreCase(String prefix) {
			return this.getViewed().streamIgnoreCase(prefix);
		}

		protected abstract MutableStringMultiTrie<V> getViewed();
	}
}

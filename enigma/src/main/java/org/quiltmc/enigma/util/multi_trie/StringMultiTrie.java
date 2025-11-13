package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;

/**
 * A {@link MutableMultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds convenience methods for accessing contents by passing a {@link String} instead of passing individual
 * characters to nodes:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #put(String, Object)}
 *     <li> {@link #remove(String, Object)}
 *     <li> {@link #removeAll(String)}
 * </ul>
 *
 * <p> {@linkplain #getView() Views} also provide a {@link #get(String)} method.
 *
 * @param <V> the type of values
 * @param <N> the type of nodes
 */
public abstract class StringMultiTrie<V, N extends MutableMapNode<Character, V, N>>
		implements MutableMultiTrie<Character, V, N> {
	private static final String PREFIX = "prefix";
	private static final String STRING = "string";
	private static final String VALUE = "value";

	@Override
	@Nonnull
	public abstract View<V, N> getView();

	@Nonnull
	public N get(String prefix) {
		Utils.requireNonNull(prefix, PREFIX);

		N node = this.getRoot();
		for (int i = 0; i < prefix.length(); i++) {
			node = node.next(prefix.charAt(i));
		}

		return node;
	}

	@Nonnull
	public N put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		N node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			final N parent = node;
			final char key = string.charAt(i);
			node = node.getChildren().computeIfAbsent(key, ignored -> parent.createChild(key));
		}

		node.put(value);

		return node;
	}

	public boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		N node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextImpl(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.remove(value);
	}

	public boolean removeAll(String string) {
		Utils.requireNonNull(string, STRING);

		N node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextImpl(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.removeAll();
	}

	public abstract static class View<V, N extends MutableMapNode<Character, V, N>> implements MultiTrie<Character, V> {
		@Nonnull
		@Override
		public Node<Character, V> getRoot() {
			return this.getViewed().getRoot().getView();
		}

		public MultiTrie.Node<Character, V> get(String prefix) {
			return this.getViewed().get(prefix).getView();
		}

		protected abstract StringMultiTrie<V, N> getViewed();
	}
}

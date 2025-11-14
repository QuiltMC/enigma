package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

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
 * @param <B> the type of branch nodes
 */
public abstract class StringMultiTrie<V, B extends MutableMapNode.Branch<Character, V, B>>
		implements MutableMultiTrie<Character, V> {
	private static final String PREFIX = "prefix";
	private static final String STRING = "string";
	private static final String VALUE = "value";

	@Override
	public abstract MutableMapNode<Character, V, B> getRoot();

	@Override
	public abstract View<V> getView();

	public MutableMapNode<Character, V, ?> get(String prefix) {
		Utils.requireNonNull(prefix, PREFIX);

		MutableMapNode<Character, V, ?> node = this.getRoot();
		for (int i = 0; i < prefix.length(); i++) {
			node = node.next(prefix.charAt(i));
		}

		return node;
	}

	public MutableMapNode<Character, V, ?> put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		MutableMapNode<Character, V, B> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			final MutableMapNode<Character, V, B> parent = node;
			final char key = string.charAt(i);
			node = node.getBranches().computeIfAbsent(key, ignored -> parent.createBranch(key));
		}

		node.put(value);

		return node;
	}

	public boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		MutableMapNode<Character, V, B> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextBranch(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.removeLeaf(value);
	}

	public boolean removeAll(String string) {
		Utils.requireNonNull(string, STRING);

		MutableMapNode<Character, V, B> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextBranch(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.clearLeaves();
	}

	public abstract static class View<V> implements MultiTrie<Character, V> {
		@Override
		public Node<Character, V> getRoot() {
			return this.getViewed().getRoot().getView();
		}

		public MultiTrie.Node<Character, V> get(String prefix) {
			return this.getViewed().get(prefix).getView();
		}

		protected abstract StringMultiTrie<V, ?> getViewed();
	}
}

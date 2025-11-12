package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;

public abstract class StringMultiTrie<V, N extends AbstractMutableMapMultiTrie.Node<Character, V, N>>
		extends AbstractMutableMapMultiTrie<Character, V, N> {
	private static final String PREFIX = "prefix";
	private static final String STRING = "string";
	private static final String VALUE = "value";

	protected StringMultiTrie(N root) {
		super(root);
	}

	public N get(String prefix) {
		Utils.requireNonNull(prefix, PREFIX);

		N node = this.root;
		for (int i = 0; i < prefix.length(); i++) {
			node = node.next(prefix.charAt(i));
			if (node == null) {
				return null;
			}
		}

		return node;
	}

	@Nonnull
	public MultiTrie.Node<Character, V> getView(String prefix) {
		final N node = this.get(prefix);
		return node == null ? EmptyNode.get() : node.getView();
	}

	@Nonnull
	public N put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		N node = this.root;
		for (int i = 0; i < string.length(); i++) {
			final N parent = node;
			node = node.children.computeIfAbsent(string.charAt(i), ignored -> parent.createChild());
		}

		node.put(value);

		return node;
	}

	public boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		N node = this.root;
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.remove(value);
	}

	public boolean removeAll(String string) {
		Utils.requireNonNull(string, STRING);

		N node = this.root;
		for (int i = 0; i < string.length(); i++) {
			node = node.next(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.removeAll();
	}
}

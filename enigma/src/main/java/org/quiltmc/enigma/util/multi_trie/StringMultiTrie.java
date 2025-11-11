package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.Node;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

public class StringMultiTrie<V, N extends Node<V, N>>
		extends AbstractMutableMapMultiTrie<Character, String, V, N> {
	protected StringMultiTrie(N root) {
		super(root);
	}

	@Nonnull
	@Override
	public MultiTrie.Node<Character, V> get(String prefix) {
		N node = this.root;
		for (int i = 0; i < prefix.length() && node != null; i++) {
			node = node.nextImpl(prefix.charAt(i));
		}

		return node == null ? EmptyNode.get() : node;
	}

	protected abstract static class Node<V, N extends Node<V, N>>
			extends AbstractMutableMapMultiTrie.Node<Character, String, V, N> {
		protected Node(Map<Character, N> children, Collection<V> leaves) {
			super(children, leaves);
		}

		@Override
		protected FirstSplit<Character, String> splitFirst(String sequence) {
			return new FirstSplit<>(sequence.charAt(0), sequence.substring(1));
		}

		@Override
		protected boolean isEmptySequence(String sequence) {
			return sequence.isEmpty();
		}
	}
}

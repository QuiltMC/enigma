package org.quiltmc.enigma.util.collection.trie;

import com.google.common.collect.Multimap;
import org.quiltmc.enigma.util.collection.trie.StringMultiTrie.StringNode;

import java.util.Map;

public class StringMultiTrie<V, N extends StringNode<V, N>>
		extends AbstractMutableMapMultiTrie<Character, String, V, N> {
	protected StringMultiTrie(N root) {
		super(root);
	}

	@Override
	public MultiTrie.Node<Character, V> get(String prefix) {
		N node = this.root;
		for (int i = 0; i < prefix.length() && node != null; i++) {
			node = node.nextImpl(prefix.charAt(i));
		}

		return node == null ? EmptyNode.get() : node;
	}

	protected abstract static class StringNode<V, N extends StringNode<V, N>>
			extends AbstractMutableMapMultiTrie.MutableMapNode<Character, String, V, N> {
		protected StringNode(Map<Character, N> children, Multimap<Character, V> leaves) {
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

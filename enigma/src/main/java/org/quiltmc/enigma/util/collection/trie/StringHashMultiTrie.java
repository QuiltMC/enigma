package org.quiltmc.enigma.util.collection.trie;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.util.collection.trie.StringHashMultiTrie.Node;

import java.util.HashMap;
import java.util.Map;

public final class StringHashMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	private static <V> Node<V> createEmptyNode() {
		return new Node<>(new HashMap<>(), HashMultimap.create());
	}

	public StringHashMultiTrie() {
		super(createEmptyNode());
	}

	static final class Node<V> extends StringMultiTrie.StringNode<V, Node<V>> {
		private Node(Map<Character, Node<V>> children, Multimap<Character, V> leaves) {
			super(children, leaves);
		}

		@Override
		protected Node<V> createEmpty() {
			return createEmptyNode();
		}
	}
}

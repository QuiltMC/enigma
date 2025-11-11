package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.util.multi_trie.StringHashMultiTrie.Node;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class StringHashMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	private static <V> Node<V> createEmptyNode() {
		return new Node<>(new HashMap<>(), HashMultimap.create());
	}

	public StringHashMultiTrie() {
		super(createEmptyNode());
	}

	static final class Node<V> extends StringMultiTrie.Node<V, Node<V>> {
		private Node(Map<Character, Node<V>> children, Multimap<Character, V> leaves) {
			super(children, leaves);
		}

		@Override
		@Nonnull
		protected Node<V> createEmpty() {
			return createEmptyNode();
		}
	}
}

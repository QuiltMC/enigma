package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.Node;

import javax.annotation.Nullable;
import java.util.Collection;

public class StringMultiTrie<V, N extends Node<V, N>>
		extends AbstractMutableMapMultiTrie<Character, String, V, N> {
	protected StringMultiTrie(N root) {
		super(root);
	}

	protected abstract static class Node<V, N extends Node<V, N>>
			extends AbstractMutableMapMultiTrie.Node<Character, String, V, N> {
		protected Node(@Nullable Node<V, N> parent, BiMap<Character, N> children, Collection<V> leaves) {
			super(parent, children, leaves);
		}

		@Override
		public int getLength(String sequence) {
			return sequence.length();
		}

		@Override
		public Character getKey(String sequence, int index) {
			return sequence.charAt(index);
		}
	}
}

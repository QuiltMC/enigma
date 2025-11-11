package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.AbstractMapMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractMapMultiTrie<K, S, V, N extends Node<K, V, N>> implements MultiTrie<K, S, V> {
	protected final N root;

	protected AbstractMapMultiTrie(N root) {
		this.root = Utils.requireNonNull(root, "root");
	}

	@Override
	public MultiTrie.Node<K, V> getRoot() {
		return this.root;
	}

	protected static class Node<K, V, N extends Node<K, V, N>> implements MultiTrie.Node<K, V> {
		protected final Map<K, N> children;
		protected final Collection<V> leaves;

		protected Node(Map<K, N> children, Collection<V> leaves) {
			this.children = Utils.requireNonNull(children, "children");
			this.leaves = Utils.requireNonNull(leaves, "leaves");
		}

		@Override
		public Stream<V> streamLeaves() {
			return this.leaves.stream();
		}

		@Override
		public Stream<V> streamBranches() {
			return this.children.values().stream().flatMap(Node::streamValues);
		}

		@Override
		public Stream<V> streamValues() {
			return Stream.concat(this.streamLeaves(), this.streamBranches());
		}

		@Override
		@Nonnull
		public MultiTrie.Node<K, V> next(K key) {
			final Node<K, V, N> next = this.nextImpl(Utils.requireNonNull(key, "key"));
			return next == null ? EmptyNode.get() : next;
		}

		@Nullable
		protected N nextImpl(K key) {
			return this.children.get(key);
		}
	}
}

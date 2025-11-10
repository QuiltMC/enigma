package org.quiltmc.enigma.util.collection.trie;

import com.google.common.collect.Multimap;
import org.quiltmc.enigma.util.collection.trie.AbstractMapMultiTrie.MapNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractMapMultiTrie<K, S, V, N extends MapNode<K, V, N>> implements MultiTrie<K, S, V> {
	protected final N root;

	protected AbstractMapMultiTrie(N root) {
		this.root = root;
	}

	@Override
	public Node<K, V> getRoot() {
		return null;
	}

	protected static class MapNode<K, V, N extends MapNode<K, V, N>> implements Node<K, V> {
		protected final Map<K, N> children;
		protected final Multimap<K, V> leaves;

		protected MapNode(Map<K, N> children, Multimap<K, V> leaves) {
			this.children = children;
			this.leaves = leaves;
		}

		@Override
		public Stream<V> streamLeaves() {
			return this.leaves.values().stream();
		}

		@Override
		public Stream<V> streamBranches() {
			return this.children.values().stream().flatMap(MapNode::streamValues);
		}

		@Override
		public Stream<V> streamValues() {
			return Stream.concat(this.streamLeaves(), this.streamBranches());
		}

		@Override
		@Nonnull
		public Node<K, V> next(K key) {
			final MapNode<K, V, N> next = this.nextImpl(key);
			return next == null ? EmptyNode.get() : next;
		}

		@Nullable
		protected N nextImpl(K key) {
			return this.children.get(key);
		}
	}
}

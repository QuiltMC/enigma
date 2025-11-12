package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.AbstractMapMultiTrie.Node;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Stream;

public abstract class AbstractMapMultiTrie<K, V, N extends Node<K, V, N>> implements MultiTrie<K, V> {
	protected final N root;

	protected AbstractMapMultiTrie(N root) {
		this.root = Utils.requireNonNull(root, "root");
	}

	@Nonnull
	@Override
	public N getRoot() {
		return this.root;
	}

	/**
	 * @param <K> the type of keys
	 * @param <V> the type of values
	 * @param <N> the type of this node
	 */
	protected abstract static class Node<K, V, N extends Node<K, V, N>> implements MultiTrie.Node<K, V> {
		protected final BiMap<K, N> children;
		protected final Collection<V> leaves;

		protected Node(BiMap<K, N> children, Collection<V> leaves) {
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
		public N next(K key) {
			return this.children.get(Utils.requireNonNull(key, "key"));
		}
	}
}

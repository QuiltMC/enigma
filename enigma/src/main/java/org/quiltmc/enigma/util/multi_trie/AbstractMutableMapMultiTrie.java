package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.multi_trie.AbstractMutableMapMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public abstract class AbstractMutableMapMultiTrie<K, V, N extends Node<K, V, N>>
		extends AbstractMapMultiTrie<K, V, N>
		implements MutableMultiTrie<K, V, N> {
	private final MutableMultiTrie.View<K, V> view = new MutableMultiTrie.View.Impl<>(this);

	protected AbstractMutableMapMultiTrie(N root) {
		super(root);
	}

	@Override
	public View<K, V> getView() {
		return this.view;
	}

	public abstract static class Node<K, V, N extends Node<K, V, N>>
			extends AbstractMapMultiTrie.Node<K, V, N>
			implements MutableMultiTrie.Node<K, V> {
		@Nullable
		protected final N parent;

		private final View<K, V> view = new View.Impl<>(this);

		protected Node(@Nullable N parent, BiMap<K, N> children, Collection<V> leaves) {
			super(children, leaves);
			this.parent = parent;
		}

		@Override
		public void put(V value) {
			this.leaves.add(value);
		}

		@Override
		public boolean remove(V value) {
			if (this.leaves.remove(value)) {
				this.pruneIfEmpty();

				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean removeAll() {
			final boolean hasLeaves = !this.leaves.isEmpty();
			if (hasLeaves) {
				this.leaves.clear();
				this.pruneIfEmpty();

				return true;
			} else {
				return false;
			}
		}

		@Override
		public View<K, V> getView() {
			return this.view;
		}

		protected void pruneIfEmpty() {
			if (this.parent != null && this.isEmpty()) {
				this.parent.children.inverse().remove(this.getSelf());
				this.parent.pruneIfEmpty();
			}
		}

		/**
		 * @return this node
		 */
		@Nonnull
		@Pure
		protected abstract N getSelf();

		/**
		 * @return a new, empty child node instance
		 */
		@Nonnull
		@Pure
		protected abstract N createChild();
	}
}

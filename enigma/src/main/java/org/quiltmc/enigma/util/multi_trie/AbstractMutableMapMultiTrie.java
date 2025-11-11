package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.AbstractMutableMapMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public abstract class AbstractMutableMapMultiTrie<K, S, V, N extends Node<K, S, V, N>>
		extends AbstractMapMultiTrie<K, S, V, N>
		implements MutableMultiTrie<K, S, V> {
	private static final String SEQUENCE = "sequence";
	private static final String VALUE = "value";

	private final View<K, S, V> view = new View<>(this);

	protected AbstractMutableMapMultiTrie(N root) {
		super(root);
	}

	@Nonnull
	@Override
	public MultiTrie.Node<K, V> getRoot() {
		return this.root.getView();
	}

	@Nonnull
	@Override
	public MultiTrie.Node<K, V> get(S prefix) {
		N node = this.root;
		for (int i = 0; i < node.getLength(prefix); i++) {
			node = node.nextImpl(node.getKey(prefix, i));
			if (node == null) {
				return EmptyNode.get();
			}
		}

		return node.getView();
	}

	@Override
	public void put(S sequence, V value) {
		this.root.put(Utils.requireNonNull(sequence, SEQUENCE), Utils.requireNonNull(value, VALUE), 0);
	}

	@Override
	public boolean remove(S sequence, V value) {
		return this.root.remove(Utils.requireNonNull(sequence, SEQUENCE), Utils.requireNonNull(value, VALUE), 0);
	}

	@Override
	public boolean removeAll(S sequence) {
		return this.root.removeAll(Utils.requireNonNull(sequence, SEQUENCE), 0);
	}

	@Override
	public MultiTrie<K, S, V> getView() {
		return this.view;
	}

	protected abstract static class Node<K, S, V, N extends Node<K, S, V, N>>
			extends AbstractMapMultiTrie.Node<K, V, N>
			implements MutableMultiTrie.Node<K, S, V> {
		@Nullable
		protected final Node<K, S, V, N> parent;

		private final NodeView<K, V> view = new NodeView<>(this);

		protected Node(@Nullable Node<K, S, V, N> parent, BiMap<K, N> children, Collection<V> leaves) {
			super(children, leaves);
			this.parent = parent;
		}

		@Override
		public void put(S sequence, V value, int startIndex) {
			if (this.getLength(sequence) <= startIndex) {
				this.leaves.add(value);
			} else {
				this.children
						.computeIfAbsent(this.getKey(sequence, startIndex), ignored -> this.createChild())
						.put(sequence, value, startIndex + 1);
			}
		}

		@Override
		public boolean remove(S sequence, V value, int startIndex) {
			final boolean removed = this.removeImpl(sequence, value, startIndex);
			if (removed) {
				this.pruneIfEmpty();

				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean removeAll(S sequence, int startIndex) {
			final boolean removed = this.removeAllImpl(sequence, startIndex);
			if (removed) {
				this.pruneIfEmpty();

				return true;
			} else {
				return false;
			}
		}

		@Override
		public MultiTrie.Node<K, V> getView() {
			return this.view;
		}

		protected boolean removeImpl(S sequence, V value, int startIndex) {
			if (this.getLength(sequence) <= startIndex) {
				return this.leaves.remove(value);
			} else {
				final N next = this.nextImpl(this.getKey(sequence, startIndex));
				return next != null && next.remove(sequence, value, startIndex + 1);
			}
		}

		protected boolean removeAllImpl(S sequence, int startIndex) {
			if (this.getLength(sequence) <= startIndex) {
				if (this.leaves.isEmpty()) {
					return false;
				} else {
					this.leaves.clear();

					return true;
				}
			} else {
				final N next = this.nextImpl(this.getKey(sequence, startIndex));
				return next != null && next.removeAll(sequence, startIndex + 1);
			}
		}

		protected void pruneIfEmpty() {
			if (this.parent != null && this.isEmpty()) {
				this.parent.children.inverse().remove(this.getSelf());
			}
		}

		/**
		 * @return this node
		 */
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

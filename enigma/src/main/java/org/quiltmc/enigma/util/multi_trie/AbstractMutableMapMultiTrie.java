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

	@Override
	public void put(S sequence, V value) {
		this.root.put(Utils.requireNonNull(sequence, SEQUENCE), Utils.requireNonNull(value, VALUE));
	}

	@Override
	public boolean remove(S sequence, V value) {
		return this.root.remove(Utils.requireNonNull(sequence, SEQUENCE), Utils.requireNonNull(value, VALUE));
	}

	@Override
	public boolean removeAll(S sequence) {
		return this.root.removeAll(Utils.requireNonNull(sequence, SEQUENCE));
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
		public void put(S sequence, V value) {
			if (this.isEmptySequence(sequence)) {
				this.leaves.add(value);
			} else {
				final FirstSplit<K, S> split = this.splitFirst(sequence);

				this.children
						.computeIfAbsent(split.first, ignored -> this.createEmpty())
						.put(split.suffix, value);
			}
		}

		@Override
		public boolean remove(S sequence, V value) {
			final boolean removed = this.removeImpl(sequence, value);
			if (removed) {
				this.pruneIfEmpty();

				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean removeAll(S sequence) {
			final boolean removed = this.removeAllImpl(sequence);
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

		protected boolean removeImpl(S sequence, V value) {
			if (this.isEmptySequence(sequence)) {
				return this.leaves.remove(value);
			} else {
				final FirstSplit<K, S> split = this.splitFirst(sequence);
				final N next = this.nextImpl(split.first);
				return next != null && next.remove(split.suffix, value);
			}
		}

		protected boolean removeAllImpl(S sequence) {
			if (this.isEmptySequence(sequence)) {
				if (this.leaves.isEmpty()) {
					return false;
				} else {
					this.leaves.clear();

					return true;
				}
			} else {
				final FirstSplit<K, S> split = this.splitFirst(sequence);
				final N next = this.nextImpl(split.first);
				return next != null && next.removeAll(split.suffix);
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
		 * @return a new, empty node instance
		 */
		@Nonnull
		@Pure
		protected abstract N createEmpty();

		/**
		 * Splits the first key from the passed {@code sequence} and returns that key and the remaining suffix.
		 *
		 * @implNote when invoked by {@link AbstractMutableMapMultiTrie}, the passed {@code sequence} is guaranteed
		 * non-empty according to {@link #isEmptySequence(Object)}
		 */
		protected abstract FirstSplit<K, S> splitFirst(S sequence);

		protected abstract boolean isEmptySequence(S sequence);

		protected record FirstSplit<K, S>(K first, S suffix) { }
	}
}

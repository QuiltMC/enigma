package org.quiltmc.enigma.util.collection.trie;

import org.quiltmc.enigma.util.collection.trie.AbstractMutableMapMultiTrie.Node;
import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class AbstractMutableMapMultiTrie<K, S, V, N extends Node<K, S, V, N>>
		extends AbstractMapMultiTrie<K, S, V, N>
		implements MutableMultiTrie<K, S, V> {
	private final View view = new View();

	protected AbstractMutableMapMultiTrie(N root) {
		super(root);
	}

	@Override
	public void put(S sequence, V value) {
		this.root.put(sequence, value);
	}

	@Override
	public boolean remove(S sequence, V value) {
		return this.root.remove(sequence, value);
	}

	@Override
	public boolean removeAll(S sequence) {
		return this.root.removeAll(sequence);
	}

	@Override
	public MultiTrie<K, S, V> view() {
		return this.view;
	}

	protected abstract static class Node<K, S, V, N extends Node<K, S, V, N>>
			extends AbstractMapMultiTrie.Node<K, V, N>
			implements MutableMultiTrie.Node<K, S, V> {
		protected Node(Map<K, N> children, Multimap<K, V> leaves) {
			super(children, leaves);
		}

		@Override
		public void put(S sequence, V value) {
			final FirstSplit<K, S> split = this.splitFirst(sequence);
			if (this.isEmptySequence(split.suffix)) {
				this.leaves.put(split.first, value);
			} else {
				final N empty = this.createEmpty();
				empty.put(split.suffix, value);
				this.children.put(split.first, empty);
			}
		}

		@Override
		public boolean remove(S sequence, V value) {
			final FirstSplit<K, S> split = this.splitFirst(sequence);
			if (this.isEmptySequence(split.suffix)) {
				return this.leaves.remove(split.first, value);
			} else {
				for (final N child : this.children.values()) {
					if (child.remove(split.suffix, value)) {
						return true;
					}
				}

				return false;
			}
		}

		@Override
		public boolean removeAll(S sequence) {
			final FirstSplit<K, S> split = this.splitFirst(sequence);
			if (this.isEmptySequence(split.suffix)) {
				return !this.leaves.removeAll(split.first).isEmpty();
			} else {
				for (final N child : this.children.values()) {
					if (child.removeAll(split.suffix)) {
						return true;
					}
				}

				return false;
			}
		}

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

	private class View implements MultiTrie<K, S, V> {
		@Override
		public Node<K, V> getRoot() {
			return AbstractMutableMapMultiTrie.this.root;
		}

		@Nonnull
		@Override
		public Node<K, V> get(S prefix) {
			return AbstractMutableMapMultiTrie.this.get(prefix);
		}
	}
}

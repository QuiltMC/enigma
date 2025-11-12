package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.MutableMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * A multi-trie that allows modification which can also provide unmodifiable views of its contents.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface MutableMultiTrie<K, V, N extends Node<K, V>> extends MultiTrie<K, V> {
	@Nonnull
	@Override
	N getRoot();

	/**
	 * @return a live, unmodifiable view of this trie
	 */
	View<K, V> getView();

	/**
	 * A mutable node representing values associated with a {@link MutableMultiTrie}.
	 *
	 * @implNote most implementations should remove themselves from any
	 * backing data structures when the node becomes empty
	 */
	interface Node<K, V> extends MultiTrie.Node<K, V> {
		@Override
		@Nullable
		Node<K, V> next(K key);

		/**
		 * @param value a value to add to this node's leaves, associating it with the sequence leading to this node.
		 */
		void put(V value);

		/**
		 * @param value a value to remove from this node's leaves
		 *
		 * @return {@code true} if a value was removed, or {@code false} otherwise
		 */
		boolean remove(V value);

		/**
		 * Removes all leaves from this node.
		 *
		 * @return {@code true} if any values were removed, or {@code false} otherwise
		 */
		boolean removeAll();

		/**
		 * @return a live, unmodifiable view of this node
		 */
		View<K, V> getView();

		interface View<K, V> extends MultiTrie.Node<K, V> {
			@Override
			@Nonnull
			View<K, V> next(K key);

			@Override
			@Nonnull
			default View<K, V> nextOrEmpty(K key) {
				return this.next(key);
			}

			class Impl<K, V> implements View<K, V> {
				protected final Node<K, V> viewed;

				public Impl(Node<K, V> viewed) {
					this.viewed = viewed;
				}

				@Override
				public Stream<V> streamLeaves() {
					return this.viewed.streamLeaves();
				}

				@Override
				public Stream<V> streamBranches() {
					return this.viewed.streamBranches();
				}

				@Override
				public Stream<V> streamValues() {
					return this.viewed.streamValues();
				}

				@Nonnull
				@Override
				public View<K, V> next(K key) {
					final Node<K, V> next = this.viewed.next(key);
					return next == null ? EmptyNode.get() : next.getView();
				}
			}
		}
	}

	interface View<K, V> extends MultiTrie<K, V> {
		@Nonnull
		@Override
		MutableMultiTrie.Node.View<K, V> getRoot();

		class Impl<K, V> implements View<K, V> {
			protected final MutableMultiTrie<K, V, ? extends MutableMultiTrie.Node<K, V>> viewed;

			public Impl(MutableMultiTrie<K, V, ? extends MutableMultiTrie.Node<K, V>> viewed) {
				this.viewed = Utils.requireNonNull(viewed, "viewed");
			}

			@Nonnull
			@Override
			public MutableMultiTrie.Node.View<K, V> getRoot() {
				return this.viewed.getRoot().getView();
			}
		}
	}
}

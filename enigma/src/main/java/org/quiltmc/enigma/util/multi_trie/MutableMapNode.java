package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.MapMaker;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link MutableMultiTrie.Node} that stores child nodes in a {@link Map}.
 *
 * <p> Nodes are aware of their keys so that they can manage their presence in maps.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @param <B> the type of branch nodes
 */
public abstract class MutableMapNode<K, V, B extends MutableMapNode.Branch<K, V, B>>
		extends MapNode<K, V, B>
		implements MutableMultiTrie.Node<K, V> {
	/**
	 * Orphans are empty nodes.
	 *
	 * <p> They may be moved to {@link #getBranches()} when they become non-empty.
	 *
	 * @implNote Using a map with weak value references prevents memory leaks when users look up a sequence with no
	 * values and don't put any value in it.
	 */
	final Map<K, B> orphans = new MapMaker().weakValues().makeMap();

	@Override
	public Stream<V> streamLeaves() {
		return this.getLeaves().stream();
	}

	@Override
	public B next(K key) {
		final B next = this.nextImpl(Utils.requireNonNull(key, "key"));
		return next == null ? this.orphans.computeIfAbsent(key, ignored -> this.createBranch(key)) : next;
	}

	@Nullable
	protected B nextImpl(K key) {
		return this.getBranches().get(key);
	}

	@Override
	public void put(V value) {
		this.getLeaves().add(value);
	}

	@Override
	public boolean removeLeaf(V value) {
		return this.getLeaves().remove(value);
	}

	@Override
	public boolean clearLeaves() {
		final boolean hasLeaves = !this.getLeaves().isEmpty();
		if (hasLeaves) {
			this.getLeaves().clear();
			return true;
		} else {
			return false;
		}
	}

	protected boolean pruneIfEmpty(K key) {
		if (this.getBranches().get(key).isEmpty()) {
			this.getBranches().remove(key);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return a new, empty branch node instance
	 */
	@Pure
	protected abstract B createBranch(K key);

	@Pure
	protected abstract Collection<V> getLeaves();

	/**
	 *
	 * @param <K> the type of keys
	 * @param <V> the type of values
	 * @param <B> the type of this node
	 */
	protected abstract static class Branch<K, V, B extends Branch<K, V, B>> extends MutableMapNode<K, V, B> {
		@Pure
		protected abstract MutableMapNode<K, V, B> getParent();

		@Pure
		protected abstract K getKey();

		/**
		 * @return this branch
		 */
		@Pure
		protected abstract B getSelf();

		@Override
		public void put(V value) {
			super.put(value);
			final boolean wasOrphan = this.getParent().orphans.remove(this.getKey()) != null;
			if (wasOrphan) {
				this.getParent().getBranches().put(this.getKey(), this.getSelf());
			}
		}

		@Override
		public boolean removeLeaf(V value) {
			if (this.getLeaves().remove(value)) {
				this.getParent().pruneIfEmpty(this.getKey());

				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean clearLeaves() {
			if (super.clearLeaves()) {
				this.getParent().pruneIfEmpty(this.getKey());

				return true;
			} else {
				return false;
			}
		}

		@Override
		protected boolean pruneIfEmpty(K key) {
			if (super.pruneIfEmpty(key)) {
				this.getParent().pruneIfEmpty(this.getKey());

				return true;
			} else {
				return false;
			}
		}
	}
}

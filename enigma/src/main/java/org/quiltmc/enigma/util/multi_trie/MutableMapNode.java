package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.MapMaker;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link MutableMultiTrie.Node} that stores branch nodes in a {@link Map}.
 *
 * <p> Branch nodes are aware of their keys so that they can help their parents manage their presence in maps for
 * trimming of empty nodes and adoption of orphan nodes.
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
	 * @implNote Keeping orphans in this map ensures there is only ever one node corresponding to a given sequence,
	 * avoiding any need to merge multiple nodes corresponding to the same sequence.<br>
	 * Using a map with weak value references prevents memory leaks when users look up a sequence with no
	 * values and don't put any value in it.
	 */
	final Map<K, B> orphans = new MapMaker().weakValues().makeMap();

	@Override
	public Stream<V> streamLeaves() {
		return this.getLeaves().stream();
	}

	@Override
	public B next(K key) {
		final B next = this.nextBranch(Utils.requireNonNull(key, "key"));
		return next == null ? this.orphans.computeIfAbsent(key, this::createBranch) : next;
	}

	@Nullable
	protected B nextBranch(K key) {
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

	/**
	 * Removes the branch node associated with the passed {@code key} if that node is empty.
	 *
	 * @implNote This should only be passed one of <em>this</em> node's branches.
	 *
	 * @return {@code true} if the branch was pruned, or {@code false otherwise}
	 */
	protected boolean pruneIfEmpty(Branch<K, V, B> branch) {
		if (branch.isEmpty()) {
			this.getBranches().remove(branch.getKey());

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
	 * A non-root node.
	 *
	 * <p> Adds logic for managing its orphan status and propagating pruning upwards.
	 *
	 * @param <K> the type of keys
	 * @param <V> the type of values
	 * @param <B> the type of this branch and of this branch's branches
	 */
	protected abstract static class Branch<K, V, B extends Branch<K, V, B>> extends MutableMapNode<K, V, B> {
		/**
		 * @return this branch's parent; may or may not be another branch node
		 */
		@Pure
		protected abstract MutableMapNode<K, V, B> getParent();

		/**
		 * @return the last key in this branch's sequence; the key this branch's parent stores it under
		 */
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
				this.getParent().pruneIfEmpty(this);

				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean clearLeaves() {
			if (super.clearLeaves()) {
				this.getParent().pruneIfEmpty(this);

				return true;
			} else {
				return false;
			}
		}

		@Override
		protected boolean pruneIfEmpty(Branch<K, V, B> branch) {
			if (super.pruneIfEmpty(branch)) {
				this.getParent().pruneIfEmpty(this);

				return true;
			} else {
				return false;
			}
		}
	}
}

package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.MapMaker;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.MutableMapNode.Branch;

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
public abstract class MutableMapNode<K, V, B extends Branch<K, V, B>> implements MutableMultiTrie.Node<K, V> {
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
	private final Map<K, B> orphans = new MapMaker().weakValues().makeMap();

	@Override
	public Stream<V> streamStems() {
		return this.getBranches().values().stream().flatMap(MutableMapNode::streamValues);
	}

	@Override
	public Stream<V> streamValues() {
		return Stream.concat(this.streamLeaves(), this.streamStems());
	}

	/**
	 * Implementations should be pure (stateless, no side effects).
	 */
	protected abstract Map<K, B> getBranches();

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
	 * @implNote This should only be passed one of <em>this</em> node's {@linkplain #getBranches() branches}.
	 *
	 * @return {@code true} if the branch was pruned, or {@code false otherwise}
	 */
	protected boolean pruneIfEmpty(Branch<K, V, B> branch) {
		if (branch.isEmpty()) {
			final K key = branch.getKey();
			final B removed = this.getBranches().remove(key);
			if (removed != null) {
				// put back in orphans in case a user is still holding a reference
				this.orphans.put(key, removed);
			}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * If the passed {@code branch} is an {@linkplain #orphans orphan},
	 * removes it from {@linkplain #orphans} and puts it in {@linkplain #getBranches() branches}.
	 *
	 * @implNote only non-empty branches should be passed to this method;
	 * it's called when a node may have changed from empty to non-empty
	 *
	 * @return {@code true} if the passed {@code branch} was an orphan, or {@code false} otherwise
	 */
	protected boolean adoptIfOrphan(Branch<K, V, B> branch) {
		final B orphan = this.orphans.remove(branch.getKey());
		if (orphan != null) {
			this.getBranches().put(branch.getKey(), orphan);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Implementations should be pure (stateless, no side effects).
	 *
	 * @return a new, empty branch node instance
	 */
	protected abstract B createBranch(K key);

	/**
	 * Implementations should be pure (stateless, no side effects).
	 */
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
		 * Implementations should be pure (stateless, no side effects).
		 *
		 * @return this branch's parent; may or may not be another branch node
		 */
		protected abstract MutableMapNode<K, V, B> getParent();

		/**
		 * Implementations should be pure (stateless, no side effects).
		 *
		 * @return the last key in this branch's sequence; the key this branch's parent stores it under
		 */
		protected abstract K getKey();

		@Override
		public void put(V value) {
			super.put(value);
			this.getParent().adoptIfOrphan(this);
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

		@Override
		protected boolean adoptIfOrphan(Branch<K, V, B> branch) {
			if (super.adoptIfOrphan(branch)) {
				this.getParent().adoptIfOrphan(this);

				return true;
			} else {
				return false;
			}
		}
	}
}

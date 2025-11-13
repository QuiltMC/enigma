package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.CompositeBiMap;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link MutableMultiTrie.Node} that stores child nodes in a {@link BiMap}.
 *
 * @implNote A {@link BiMap} is used to facilitate pruning of empty nodes; child nodes can remove themselves from their
 * parents without knowing their key.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @param <N> the type of this node
 */
public abstract class MutableMapNode<K, V, N extends MutableMapNode<K, V, N>>
		extends MapNode<K, V, N>
		implements MutableMultiTrie.Node<K, V> {
	@Override
	public Stream<V> streamLeaves() {
		return this.getLeaves().stream();
	}

	/**
	 * Orphans are empty nodes.
	 *
	 * <p> They may be moved to {@link #getChildren()} when they become non-empty.
	 *
	 * @implNote Using a map with weak value references prevents memory leaks when users look up a sequence with no
	 * values and don't put any value in it.
	 */
	final BiMap<K, N> orphans = CompositeBiMap.ofWeakValues();

	@Override
	@Nonnull
	public N next(K key) {
		final N next = this.nextImpl(Utils.requireNonNull(key, "key"));
		return next == null ? this.orphans.computeIfAbsent(key, ignored -> this.createChild()) : next;
	}

	protected N nextImpl(K key) {
		return this.getChildren().get(key);
	}

	@Override
	public void put(V value) {
		this.getLeaves().add(value);
		this.getParent().ifPresent(parent -> {
			final N self = this.getSelf();
			final K key = parent.orphans.inverse().remove(self);
			if (key != null) {
				parent.getChildren().put(key, self);
			}
		});
	}

	@Override
	public boolean remove(V value) {
		if (this.getLeaves().remove(value)) {
			this.pruneIfEmpty();

			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeAll() {
		final boolean hasLeaves = !this.getLeaves().isEmpty();
		if (hasLeaves) {
			this.getLeaves().clear();
			this.pruneIfEmpty();

			return true;
		} else {
			return false;
		}
	}

	protected void pruneIfEmpty() {
		this.getParent().ifPresent(parent -> {
			if (this.isEmpty()) {
				parent.getChildren().inverse().remove(this.getSelf());
				parent.pruneIfEmpty();
			}
		});
	}

	@Override
	@Nonnull
	protected abstract BiMap<K, N> getChildren();

	/**
	 * @return this node
	 */
	@Nonnull
	@Pure
	protected abstract N getSelf();

	@Pure
	protected abstract Optional<N> getParent();

	/**
	 * @return a new, empty child node instance
	 */
	@Nonnull
	@Pure
	protected abstract N createChild();

	protected abstract Collection<V> getLeaves();
}

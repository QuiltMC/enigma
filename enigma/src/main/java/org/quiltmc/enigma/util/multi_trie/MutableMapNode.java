package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.MapMaker;
import org.checkerframework.dataflow.qual.Pure;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link MutableMultiTrie.Node} that stores child nodes in a {@link Map}.
 *
 * <p> Nodes are aware of their keys so that they can manage their presence in maps.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @param <N> the type of this node
 */
public abstract class MutableMapNode<K, V, N extends MutableMapNode<K, V, N>>
		extends MapNode<K, V, N>
		implements MutableMultiTrie.Node<K, V> {
	/**
	 * Orphans are empty nodes.
	 *
	 * <p> They may be moved to {@link #getChildren()} when they become non-empty.
	 *
	 * @implNote Using a map with weak value references prevents memory leaks when users look up a sequence with no
	 * values and don't put any value in it.
	 */
	final Map<K, N> orphans = new MapMaker().weakValues().makeMap();

	@Override
	public Stream<V> streamLeaves() {
		return this.getLeaves().stream();
	}

	@Override
	@Nonnull
	public N next(K key) {
		final N next = this.nextImpl(Utils.requireNonNull(key, "key"));
		return next == null ? this.orphans.computeIfAbsent(key, ignored -> this.createChild(key)) : next;
	}

	protected N nextImpl(K key) {
		return this.getChildren().get(key);
	}

	@Override
	public void put(V value) {
		this.getLeaves().add(value);
		this.getParentAccess().ifPresent(access -> {
			final boolean wasOrphan = access.parent.orphans.remove(access.key) != null;
			if (wasOrphan) {
				access.parent.getChildren().put(access.key, this.getSelf());
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
		this.getParentAccess().ifPresent(access -> {
			if (this.isEmpty()) {
				access.parent.getChildren().remove(access.key);
				access.parent.pruneIfEmpty();
			}
		});
	}

	/**
	 * @return this node
	 */
	@Nonnull
	@Pure
	protected abstract N getSelf();

	@Pure
	protected abstract Optional<ParentAccess<N, K>> getParentAccess();

	/**
	 * @return a new, empty child node instance
	 */
	@Nonnull
	@Pure
	protected final N createChild(K key) {
		return this.createChildImpl(new ParentAccess<>(this.getSelf(), key));
	}

	protected abstract N createChildImpl(ParentAccess<N, K> parentAccess);

	protected abstract Collection<V> getLeaves();

	protected record ParentAccess<N, K>(N parent, K key) { }
}

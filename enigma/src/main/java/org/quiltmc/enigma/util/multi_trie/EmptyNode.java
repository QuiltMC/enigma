package org.quiltmc.enigma.util.multi_trie;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * An empty, immutable, singleton {@link MultiTrie.Node} implementation.
 *
 * <p> {@link MultiTrie.Node#next(Object)} implementations may return {@linkplain #get() the empty node}
 * when nodes have no branches.
 *
 * @implNote <em>not</em> intended to be stored in tries
 */
public final class EmptyNode<K, V> implements MultiTrie.Node<K, V> {
	private static final EmptyNode<?, ?> INSTANCE = new EmptyNode<>();

	@SuppressWarnings("unchecked")
	public static <K, V> EmptyNode<K, V> get() {
		return (EmptyNode<K, V>) INSTANCE;
	}

	private EmptyNode() { }

	@Override
	public Stream<V> streamLeaves() {
		return Stream.empty();
	}

	@Override
	public Stream<V> streamBranches() {
		return Stream.empty();
	}

	@Override
	public Stream<V> streamValues() {
		return Stream.empty();
	}

	@Override
	@Nonnull
	public MultiTrie.Node<K, V> next(K key) {
		return this;
	}
}

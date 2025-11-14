package org.quiltmc.enigma.util.multi_trie;

import org.checkerframework.dataflow.qual.Pure;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link MultiTrie.Node} that stores child nodes in a {@link Map}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @param <N> the type of this node
 */
public abstract class MapNode<K, V, N extends MapNode<K, V, N>> implements MultiTrie.Node<K, V> {
	@Override
	public Stream<V> streamStems() {
		return this.getBranches().values().stream().flatMap(MapNode::streamValues);
	}

	@Override
	public Stream<V> streamValues() {
		return Stream.concat(this.streamLeaves(), this.streamStems());
	}

	@Pure
	protected abstract Map<K, N> getBranches();
}

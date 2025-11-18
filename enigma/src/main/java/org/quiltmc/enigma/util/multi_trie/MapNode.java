package org.quiltmc.enigma.util.multi_trie;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link MultiTrie.Node} that stores branch nodes in a {@link Map}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @param <B> the type of branch nodes
 */
public abstract class MapNode<K, V, B extends MapNode<K, V, B>> implements MultiTrie.Node<K, V> {
	@Override
	public Stream<V> streamStems() {
		return this.getBranches().values().stream().flatMap(MapNode::streamValues);
	}

	@Override
	public Stream<V> streamValues() {
		return Stream.concat(this.streamLeaves(), this.streamStems());
	}

	/**
	 * Implementations should be pure (stateless, no side effects).
	 */
	protected abstract Map<K, B> getBranches();
}

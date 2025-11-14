package org.quiltmc.enigma.util.multi_trie;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public final class NodeView<K, V> implements MultiTrie.Node<K, V> {
	private final MutableMultiTrie.Node<K, V> viewed;

	public NodeView(MutableMultiTrie.Node<K, V> viewed) {
		this.viewed = viewed;
	}

	@Override
	public Stream<V> streamLeaves() {
		return this.viewed.streamLeaves();
	}

	@Override
	public Stream<V> streamStems() {
		return this.viewed.streamStems();
	}

	@Override
	public Stream<V> streamValues() {
		return this.viewed.streamValues();
	}

	@Nonnull
	@Override
	public MultiTrie.Node<K, V> next(K key) {
		return this.viewed.next(key).getView();
	}
}

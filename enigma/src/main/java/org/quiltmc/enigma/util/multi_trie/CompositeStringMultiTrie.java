package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Node;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return new CompositeStringMultiTrie<>(HashMap::new, HashSet::new);
	}

	private static <V> Node<V> createNode(
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Node<>(childrenFactory.get(), leavesFactory.get(), () -> createNode(childrenFactory, leavesFactory));
	}

	private CompositeStringMultiTrie(
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		super(createNode(childrenFactory, leavesFactory));
	}

	protected static final class Node<V> extends StringMultiTrie.Node<V, Node<V>> {
		private final Supplier<Node<V>> factory;

		private Node(Map<Character, Node<V>> children, Collection<V> leaves, Supplier<Node<V>> factory) {
			super(children, leaves);

			this.factory = factory;
		}

		@Nonnull
		@Override
		protected Node<V> createEmpty() {
			return this.factory.get();
		}
	}
}

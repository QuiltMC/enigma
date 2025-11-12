package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return new CompositeStringMultiTrie<>(HashBiMap::create, HashSet::new);
	}

	private static <V> Node<V> createNode(
			@Nullable Node<V> parent,
			Supplier<BiMap<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Node<>(
				parent,
				childrenFactory.get(), leavesFactory.get(),
				self -> createNode(self, childrenFactory, leavesFactory)
		);
	}

	private CompositeStringMultiTrie(
			Supplier<BiMap<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		super(createNode(null, childrenFactory, leavesFactory));
	}

	public static final class Node<V> extends AbstractMutableMapMultiTrie.Node<Character, V, Node<V>> {
		private final UnaryOperator<Node<V>> childFactory;

		// private final View view = new View();

		private Node(
				@Nullable Node<V> parent, BiMap<Character, Node<V>> children, Collection<V> leaves,
				UnaryOperator<Node<V>> childFactory
		) {
			super(parent, children, leaves);

			this.childFactory = childFactory;
		}

		@Nonnull
		@Override
		protected Node<V> getSelf() {
			return this;
		}

		@Nonnull
		@Override
		protected Node<V> createChild() {
			return this.childFactory.apply(this);
		}
	}
}

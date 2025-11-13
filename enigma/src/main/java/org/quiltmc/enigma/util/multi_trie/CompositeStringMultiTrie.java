package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	private final Node<V> root;
	private final View view = new View();

	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return of(HashBiMap::create, HashSet::new);
	}

	public static <V> CompositeStringMultiTrie<V> of(
			Supplier<BiMap<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new CompositeStringMultiTrie<>(childrenFactory, leavesFactory);
	}

	private static <V> Node<V> createNode(
			@Nullable Node<V> parent,
			Supplier<BiMap<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Node<>(
				parent, childrenFactory.get(), leavesFactory.get(),
				self -> createNode(self, childrenFactory, leavesFactory)
		);
	}

	private CompositeStringMultiTrie(
			Supplier<BiMap<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		this.root = createNode(null, childrenFactory, leavesFactory);
	}

	@Nonnull
	@Override
	public Node<V> getRoot() {
		return this.root;
	}

	@Override
	@Nonnull
	public StringMultiTrie.View<V, Node<V>> getView() {
		return this.view;
	}

	public static final class Node<V> extends MutableMapNode<Character, V, Node<V>> {
		@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
		private final Optional<Node<V>> parent;

		private final BiMap<Character, Node<V>> children;
		private final Collection<V> leaves;

		private final UnaryOperator<Node<V>> childFactory;

		private final NodeView<Character, V> view = new NodeView<>(this);

		private Node(
				@Nullable Node<V> parent,
				BiMap<Character, Node<V>> children, Collection<V> leaves,
				UnaryOperator<Node<V>> childFactory
		) {
			this.parent = Optional.ofNullable(parent);
			this.children = children;
			this.leaves = leaves;
			this.childFactory = childFactory;
		}

		@Nonnull
		@Override
		protected Node<V> getSelf() {
			return this;
		}

		@Override
		protected Optional<Node<V>> getParent() {
			return this.parent;
		}

		@Nonnull
		@Override
		protected Node<V> createChild() {
			return this.childFactory.apply(this);
		}

		@Override
		protected Collection<V> getLeaves() {
			return this.leaves;
		}

		@Override
		@Nonnull
		protected BiMap<Character, Node<V>> getChildren() {
			return this.children;
		}

		@Override
		public MultiTrie.Node<Character, V> getView() {
			return this.view;
		}
	}

	private class View extends StringMultiTrie.View<V, Node<V>> {
		@Override
		protected StringMultiTrie<V, CompositeStringMultiTrie.Node<V>> getViewed() {
			return CompositeStringMultiTrie.this;
		}
	}
}

package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Node;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, Node<V>> {
	private final Node<V> root;
	private final View view = new View();

	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return of(HashMap::new, HashSet::new);
	}

	public static <V> CompositeStringMultiTrie<V> of(
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new CompositeStringMultiTrie<>(childrenFactory, leavesFactory);
	}

	private static <V> Node<V> createRoot(
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Node<>(
				Optional.empty(), childrenFactory.get(), leavesFactory.get(),
				selfAccess -> createNode(selfAccess, childrenFactory, leavesFactory)
		);
	}

	private static <V> Node<V> createNode(
			MutableMapNode.ParentAccess<Node<V>, Character> parentAccess,
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Node<>(
				Optional.of(parentAccess), childrenFactory.get(), leavesFactory.get(),
				selfAccess -> createNode(selfAccess, childrenFactory, leavesFactory)
		);
	}

	private CompositeStringMultiTrie(
			Supplier<Map<Character, Node<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		this.root = createRoot(childrenFactory, leavesFactory);
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

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static final class Node<V> extends MutableMapNode<Character, V, Node<V>> {
		private final Optional<ParentAccess<Node<V>, Character>> parentAccess;

		private final Map<Character, Node<V>> children;
		private final Collection<V> leaves;

		private final Function<ParentAccess<Node<V>, Character>, Node<V>> childFactory;

		private final NodeView<Character, V> view = new NodeView<>(this);

		private Node(
				Optional<ParentAccess<Node<V>, Character>> parentAccess,
				Map<Character, Node<V>> children, Collection<V> leaves,
				Function<ParentAccess<Node<V>, Character>, Node<V>> childFactory
		) {
			this.parentAccess = parentAccess;
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
		protected Optional<ParentAccess<Node<V>, Character>> getParentAccess() {
			return this.parentAccess;
		}

		@Nonnull
		@Override
		protected Node<V> createChildImpl(ParentAccess<Node<V>, Character> parentAccess) {
			return this.childFactory.apply(parentAccess);
		}

		@Override
		protected Collection<V> getLeaves() {
			return this.leaves;
		}

		@Override
		@Nonnull
		protected Map<Character, Node<V>> getChildren() {
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

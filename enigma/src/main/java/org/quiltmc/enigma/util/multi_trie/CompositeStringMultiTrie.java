package org.quiltmc.enigma.util.multi_trie;

import com.google.common.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link StringMultiTrie} that allows customization of nodes' backing data structures.
 *
 * @param <V> the type of values
 *
 * @see #of(Supplier, Supplier)
 * @see #createHashed()
 */
public final class CompositeStringMultiTrie<V> implements MutableStringMultiTrie<V> {
	private final Root<V> root;
	private final View view = new View();

	/**
	 * Creates a trie with nodes whose branches are held in {@link HashMap}s
	 * and whose leaves are held in {@link HashSet}s.
	 *
	 * @param <V> the type of values stored in the created trie
	 *
	 * @see #of(Supplier, Supplier)
	 */
	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return of(HashMap::new, HashSet::new);
	}

	/**
	 * Creates a trie with nodes whose branches are held in maps created by the passed {@code branchesFactory}
	 * and whose leaves are held in collections created by the passed {@code leavesFactory}.
	 *
	 * @param branchesFactory a pure method that creates a new, empty {@link Map} in which to hold branch nodes
	 * @param leavesFactory   a pure method that create a new, empty {@link Collection} in which to hold leaf values
	 *
	 * @param <V> the type of values stored in the created trie
	 *
	 * @see #createHashed()
	 */
	public static <V> CompositeStringMultiTrie<V> of(
			Supplier<Map<Character, Branch<V>>> branchesFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new CompositeStringMultiTrie<>(branchesFactory, leavesFactory);
	}

	private CompositeStringMultiTrie(
			Supplier<Map<Character, Branch<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		this.root = new Root<>(
			childrenFactory.get(), leavesFactory.get(),
			new Branch.Factory<>(leavesFactory, childrenFactory)
		);
	}

	@Override
	public Node<V> getRoot() {
		return this.root;
	}

	@Override
	public StringMultiTrie<V> view() {
		return this.view;
	}

	@VisibleForTesting
	static final class Root<V>
			extends MutableMapNode<Character, V, Branch<V>>
			implements Node<V> {
		private final Collection<V> leaves;
		private final Map<Character, CompositeStringMultiTrie.Branch<V>> branches;

		private final CompositeStringMultiTrie.Branch.Factory<V> branchFactory;

		private final NodeView<V> view = new NodeView<>(this);

		private Root(
				Map<Character, CompositeStringMultiTrie.Branch<V>> branches, Collection<V> leaves,
				CompositeStringMultiTrie.Branch.Factory<V> branchFactory
		) {
			this.leaves = leaves;
			this.branches = branches;
			this.branchFactory = branchFactory;
		}

		@Override
		public Node<V> previous() {
			return this;
		}

		@Override
		public Node<V> previous(int steps) {
			return this;
		}

		@Override
		public int getDepth() {
			return 0;
		}

		@Override
		protected CompositeStringMultiTrie.Branch<V> createBranch(Character key) {
			return this.branchFactory.create(key, this);
		}

		@Override
		protected Collection<V> getLeaves() {
			return this.leaves;
		}

		@Override
		protected Map<Character, CompositeStringMultiTrie.Branch<V>> getBranches() {
			return this.branches;
		}

		@Override
		public StringMultiTrie.Node<V> view() {
			return this.view;
		}
	}

	public static final class Branch<V> extends MutableMapNode.Branch<Character, V, Branch<V>> implements Node<V> {
		private final MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> parent;
		private final Node<V> previous;

		private final Character key;
		private final int depth;

		private final Collection<V> leaves;
		private final Map<Character, CompositeStringMultiTrie.Branch<V>> branches;

		private final CompositeStringMultiTrie.Branch.Factory<V> branchFactory;

		private final NodeView<V> view = new NodeView<>(this);

		private <P extends MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> & Node<V>> Branch(
				P parent, char key,
				int depth, Collection<V> leaves, Map<Character, CompositeStringMultiTrie.Branch<V>> branches,
				Factory<V> branchFactory
		) {
			// two references to the same instance because both its types are necessary
			this.parent = parent;
			this.previous = parent;

			this.key = key;
			this.depth = depth;

			this.leaves = leaves;
			this.branches = branches;
			this.branchFactory = branchFactory;
		}

		@Override
		public Node<V> previous() {
			return this.previous;
		}

		@Override
		public Node<V> previous(int steps) {
			return MultiTrie.Node.<Character, V, Node<V>>
				previous(this, steps, Node::previous);
		}

		@Override
		public int getDepth() {
			return this.depth;
		}

		@Override
		protected MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> getParent() {
			return this.parent;
		}

		@Override
		protected Character getKey() {
			return this.key;
		}

		@Override
		protected CompositeStringMultiTrie.Branch<V> createBranch(Character key) {
			return this.branchFactory.create(key, this);
		}

		@Override
		protected Collection<V> getLeaves() {
			return this.leaves;
		}

		@Override
		protected Map<Character, CompositeStringMultiTrie.Branch<V>> getBranches() {
			return this.branches;
		}

		@Override
		public StringMultiTrie.Node<V> view() {
			return this.view;
		}

		private record Factory<V>(
				Supplier<Collection<V>> leavesFactory,
				Supplier<Map<Character, CompositeStringMultiTrie.Branch<V>>> branchesFactory
		) {
			<P extends MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> & Node<V>>
					CompositeStringMultiTrie.Branch<V> create(char key, P parent) {
				return new CompositeStringMultiTrie.Branch<>(
						parent, key, parent.getDepth() + 1, this.leavesFactory.get(), this.branchesFactory.get(),
						this
				);
			}
		}
	}

	private class View extends AbstractView<V> {
		@Override
		protected CompositeStringMultiTrie<V> getViewed() {
			return CompositeStringMultiTrie.this;
		}
	}

	private static final class NodeView<V>
			extends MutableMultiTrie.Node.View<Character, V>
			implements StringMultiTrie.Node<V> {
		final Node<V> viewed;

		NodeView(Node<V> viewed) {
			this.viewed = viewed;
		}

		@Override
		public StringMultiTrie.Node<V> next(Character key) {
			return this.viewed.next(key).view();
		}

		@Override
		public StringMultiTrie.Node<V> previous() {
			return this.viewed.previous().view();
		}

		@Override
		public StringMultiTrie.Node<V> previous(int steps) {
			return this.viewed.previous(steps).view();
		}

		@Override
		protected MutableMultiTrie.Node<Character, V> getViewed() {
			return this.viewed;
		}
	}
}

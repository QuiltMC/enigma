package org.quiltmc.enigma.util.multi_trie;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link StringMultiTrie} that allows customization of nodes' backing data structures.
 *
 * @param <V> the type of values
 *
 * @see #of(Supplier, BranchesFactory)
 * @see #createHashed()
 */
public final class CompositeStringMultiTrie<V> implements MutableStringMultiTrie<V> {
	private static final int HASHED_NODE_MIN_INITIAL_CAPACITY = 1;
	private static final int HASHED_ROOT_INITIAL_CAPACITY_POWER = 5;
	// 32
	private static final int HASHED_ROOT_INITIAL_CAPACITY =
			HASHED_NODE_MIN_INITIAL_CAPACITY << HASHED_ROOT_INITIAL_CAPACITY_POWER;

	private final Root<V> root;
	private final View view = new View();

	/**
	 * Creates a trie with nodes whose branches are held in {@link HashMap}s
	 * and whose leaves are held in {@link HashSet}s.
	 *
	 * @param <V> the type of values stored in the created trie
	 *
	 * @see #of(Supplier, BranchesFactory)
	 */
	public static <V> CompositeStringMultiTrie<V> createHashed() {
		// decrease minimum capacity by a factor of 2 at each depth
		return createHashedBranching(HashSet::new);
	}

	/**
	 * Creates a trie with nodes whose branches are held in {@link HashMap}s
	 * and whose leaves are held in collections created by the passed {@code leavesFactory}.
	 *
	 * @param leavesFactory a stateless method that creates a new, empty {@link Collection}
	 *                      in which to hold leaf values with each call
	 *
	 * @param <V> the type of values stored in the created trie
	 *
	 * @see #of(Supplier, BranchesFactory)
	 * @see #createHashed()
	 */
	public static <V> CompositeStringMultiTrie<V> createHashedBranching(Supplier<Collection<V>> leavesFactory) {
		return of(leavesFactory, CompositeStringMultiTrie::hashedBranchesAt);
	}

	/**
	 * Creates a trie with nodes whose branches are held in maps created by the passed {@code branchesFactory}
	 * and whose leaves are held in collections created by the passed {@code leavesFactory}.
	 *
	 * @param leavesFactory   a stateless method that creates a new, empty {@link Collection}
	 *                        in which to hold leaf values with each call
	 * @param branchesFactory a stateless method that creates a new, empty {@link Map}
	 *                        in which to hold branch nodes with each call; it receives the depth of the node
	 *
	 * @param <V> the type of values stored in the created trie
	 *
	 * @see #createHashed()
	 */
	public static <V> CompositeStringMultiTrie<V> of(
			Supplier<Collection<V>> leavesFactory,
			BranchesFactory<V> branchesFactory
	) {
		return new CompositeStringMultiTrie<>(leavesFactory, branchesFactory);
	}

	private CompositeStringMultiTrie(Supplier<Collection<V>> leavesFactory, BranchesFactory<V> branchesFactory) {
		this.root = new Root<>(
			branchesFactory.create(Root.DEPTH), leavesFactory.get(),
			new Branch.Factory<>(leavesFactory, branchesFactory)
		);
	}

	private static <B> Map<Character, B> hashedBranchesAt(int depth) {
		// decrease minimum capacity by a factor of 2 at each depth
		return new HashMap<>(depth >= HASHED_ROOT_INITIAL_CAPACITY_POWER
				? HASHED_NODE_MIN_INITIAL_CAPACITY
				: HASHED_ROOT_INITIAL_CAPACITY >> depth
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

	private static final class Root<V> extends MutableMapNode<Character, V, Branch<V>> implements Node<V> {
		private static final int DEPTH = 0;

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
			return DEPTH;
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

	private static final class Branch<V> extends MutableMapNode.Branch<Character, V, Branch<V>> implements Node<V> {
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
				BranchesFactory<V> branchesFactory
		) {
			<P extends MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> & Node<V>>
					CompositeStringMultiTrie.Branch<V> create(char key, P parent) {
				final int depth = parent.getDepth() + 1;
				return new CompositeStringMultiTrie.Branch<>(
						parent, key, depth,
						this.leavesFactory.get(), this.branchesFactory.create(depth),
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
		public Stream<StringMultiTrie.Node<V>> streamNextIgnoreCase(Character key) {
			return this.viewed.streamNextIgnoreCase(key);
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

	/**
	 * A method that creates a map in which to hold a {@link MutableStringMultiTrie.Node Node}'s branch nodes.
	 *
	 * @param <V> the type of values held by {@link MutableStringMultiTrie.Node Node}s
	 *
	 * @implSpec Implementations should be stateless and produce a new, empty map with each call to
	 * {@link #create(int)}.
	 */
	@FunctionalInterface
	public interface BranchesFactory<V> {
		/**
		 * @param depth the depth of the node whose branches map is being created
		 *
		 * @param <B> the type of branches; the exact type is hidden from implementers
		 *
		 * @return a new, empty map in which to hold a {@link MutableStringMultiTrie.Node Node}'s branches
		 */
		<B extends MutableStringMultiTrie.Node<V>> Map<Character, B> create(int depth);
	}
}

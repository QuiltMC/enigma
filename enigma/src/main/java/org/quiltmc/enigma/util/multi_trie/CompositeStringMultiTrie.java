package org.quiltmc.enigma.util.multi_trie;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, CompositeStringMultiTrie.Branch<V>> {
	private final Root<V> root;
	private final View view = new View();

	public static <V> CompositeStringMultiTrie<V> createHashed() {
		return of(HashMap::new, HashSet::new);
	}

	public static <V> CompositeStringMultiTrie<V> of(
			Supplier<Map<Character, Branch<V>>> branchesFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new CompositeStringMultiTrie<>(branchesFactory, leavesFactory);
	}

	private static <V> Root<V> createRoot(
			Supplier<Map<Character, Branch<V>>> branchesFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		return new Root<>(
				branchesFactory.get(), leavesFactory.get(),
				new Branch.Factory<>(leavesFactory, branchesFactory)
		);
	}

	private CompositeStringMultiTrie(
			Supplier<Map<Character, Branch<V>>> childrenFactory,
			Supplier<Collection<V>> leavesFactory
	) {
		this.root = createRoot(childrenFactory, leavesFactory);
	}

	@Override
	public MutableMapNode<Character, V, Branch<V>> getRoot() {
		return this.root;
	}

	@Override
	public StringMultiTrie.View<V> getView() {
		return this.view;
	}

	private static final class Root<V> extends MutableMapNode<Character, V, Branch<V>> {
		private final Collection<V> leaves;
		private final Map<Character, CompositeStringMultiTrie.Branch<V>> branches;

		private final CompositeStringMultiTrie.Branch.Factory<V> branchFactory;

		private final NodeView<Character, V> view = new NodeView<>(this);

		private Root(
				Map<Character, CompositeStringMultiTrie.Branch<V>> branches, Collection<V> leaves,
				CompositeStringMultiTrie.Branch.Factory<V> branchFactory
		) {
			this.leaves = leaves;
			this.branches = branches;
			this.branchFactory = branchFactory;
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
		public MultiTrie.Node<Character, V> getView() {
			return this.view;
		}
	}

	public static final class Branch<V> extends MutableMapNode.Branch<Character, V, Branch<V>> {
		private final MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> parent;
		private final Character key;

		private final Collection<V> leaves;
		private final Map<Character, CompositeStringMultiTrie.Branch<V>> branches;

		private final CompositeStringMultiTrie.Branch.Factory<V> branchFactory;

		private final MultiTrie.Node<Character, V> view = new NodeView<>(this);

		private Branch(
				MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> parent, char key,
				Collection<V> leaves, Map<Character, CompositeStringMultiTrie.Branch<V>> branches,
				CompositeStringMultiTrie.Branch.Factory<V> branchFactory
		) {
			this.parent = parent;
			this.key = key;

			this.leaves = leaves;
			this.branches = branches;
			this.branchFactory = branchFactory;
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
		protected CompositeStringMultiTrie.Branch<V> getSelf() {
			return this;
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
		public MultiTrie.Node<Character, V> getView() {
			return this.view;
		}

		private record Factory<V>(
				Supplier<Collection<V>> leavesFactory,
				Supplier<Map<Character, CompositeStringMultiTrie.Branch<V>>> branchesFactory
		) {
			CompositeStringMultiTrie.Branch<V> create(
					char key, MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> parent
			) {
				return new CompositeStringMultiTrie.Branch<>(
						parent, key, this.leavesFactory.get(), this.branchesFactory.get(),
						new CompositeStringMultiTrie.Branch.Factory<>(this.leavesFactory, this.branchesFactory)
				);
			}
		}
	}

	private class View extends StringMultiTrie.View<V> {
		@Override
		protected StringMultiTrie<V, ?> getViewed() {
			return CompositeStringMultiTrie.this;
		}
	}
}

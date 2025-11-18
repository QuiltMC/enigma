package org.quiltmc.enigma.util.multi_trie;

import org.jspecify.annotations.NonNull;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Branch;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie.Root;

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
public final class CompositeStringMultiTrie<V> extends StringMultiTrie<V, Branch<V>, Root<V>> {
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
	public Root<V> getRoot() {
		return this.root;
	}

	@Override
	public StringMultiTrie.View<V, Branch<V>, Root<V>> view() {
		return this.view;
	}

	public static final class Root<V>
			extends MutableMapNode<Character, V, Branch<V>>
			implements MutableCharacterNode<V, Branch<V>> {
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
		public CharacterNode<V> view() {
			return this.view;
		}
	}

	public static final class Branch<V>
			extends MutableMapNode.Branch<Character, V, Branch<V>>
			implements MutableCharacterNode<V, Branch<V>> {
		private final MutableMapNode<Character, V, CompositeStringMultiTrie.Branch<V>> parent;
		private final Character key;

		private final Collection<V> leaves;
		private final Map<Character, CompositeStringMultiTrie.Branch<V>> branches;

		private final CompositeStringMultiTrie.Branch.Factory<V> branchFactory;

		private final NodeView<V> view = new NodeView<>(this);

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
		public CharacterNode<V> view() {
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
						this
				);
			}
		}
	}

	private class View extends StringMultiTrie.View<V, Branch<V>, Root<V>> {
		@Override
		protected CompositeStringMultiTrie<V> getViewed() {
			return CompositeStringMultiTrie.this;
		}
	}

	private static final class NodeView<V> extends Node.View<Character, V> implements CharacterNode<V> {
		final MutableCharacterNode<V, Branch<V>> viewed;

		NodeView(MutableCharacterNode<V, Branch<V>> viewed) {
			this.viewed = viewed;
		}

		@NonNull
		@Override
		public CharacterNode<V> next(Character key) {
			return this.viewed.next(key).view();
		}

		@Override
		protected Node<Character, V> getViewed() {
			return this.viewed;
		}
	}
}

package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.MutableCharacterNode;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link MutableMultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds {@link String}/{@link Character}-specific convenience methods for accessing its contents:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #getIgnoreCase(String)}
 *     <li> {@link #put(String, Object)}
 *     <li> {@link #remove(String, Object)}
 *     <li> {@link #removeAll(String)}
 *     <li> {@link CharacterNode#nextIgnoreCase(Character)}
 * </ul>
 *
 * <p> {@linkplain #view() Views} also provide {@link View#get(String) get} and
 * {@link View#getIgnoreCase(String) getIgnoreCase} methods.
 *
 * @param <V> the type of values
 * @param <B> the type of branch nodes
 */
public abstract class StringMultiTrie
		<
			V,
			B extends MutableMapNode.Branch<Character, V, B> & MutableCharacterNode<V, B>,
			R extends MutableMapNode<Character, V, B> & MutableCharacterNode<V, B>
		>
		implements MutableMultiTrie<Character, V> {
	protected static Optional<Character> tryToggleCase(char c) {
		if (Character.isUpperCase(c)) {
			return Optional.of(Character.toLowerCase(c));
		} else if (Character.isLowerCase(c)) {
			return Optional.of(Character.toUpperCase(c));
		} else {
			return Optional.empty();
		}
	}

	private static final String PREFIX = "prefix";
	private static final String STRING = "string";
	private static final String VALUE = "value";

	@Override
	public abstract R getRoot();

	@Override
	public abstract View<V, B, R> view();

	public MutableCharacterNode<V, B> get(String prefix) {
		return this.getImpl(prefix, MutableCharacterNode::next);
	}

	public MutableCharacterNode<V, B> getIgnoreCase(String prefix) {
		return this.getImpl(prefix, MutableCharacterNode::nextIgnoreCase);
	}

	private MutableCharacterNode<V, B> getImpl(
			String prefix, BiFunction<MutableCharacterNode<V, B>, Character, MutableCharacterNode<V, B>> next
	) {
		Utils.requireNonNull(prefix, PREFIX);

		MutableCharacterNode<V, B> node = this.getRoot();
		for (int i = 0; i < prefix.length(); i++) {
			node = next.apply(node, prefix.charAt(i));
		}

		return node;
	}

	public MutableCharacterNode<V, B> put(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		final R root = this.getRoot();
		if (string.isEmpty()) {
			root.put(value);

			return root;
		}

		// Don't use next, initially creating orphans, because we always put a value,
		// so all orphans would be adopted anyway.
		B branch = root.getBranches().computeIfAbsent(string.charAt(0), root::createBranch);
		for (int i = 1; i < string.length(); i++) {
			final B parent = branch;
			branch = branch.getBranches().computeIfAbsent(string.charAt(i), parent::createBranch);
		}

		branch.put(value);

		return branch;
	}

	public boolean remove(String string, V value) {
		Utils.requireNonNull(string, STRING);
		Utils.requireNonNull(value, VALUE);

		MutableMapNode<Character, V, B> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextBranch(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.removeLeaf(value);
	}

	public boolean removeAll(String string) {
		Utils.requireNonNull(string, STRING);

		MutableMapNode<Character, V, B> node = this.getRoot();
		for (int i = 0; i < string.length(); i++) {
			node = node.nextBranch(string.charAt(i));

			if (node == null) {
				return false;
			}
		}

		return node.clearLeaves();
	}

	public interface CharacterNode<V> extends MultiTrie.Node<Character, V> {
		@Override
		CharacterNode<V> next(Character key);

		default CharacterNode<V> nextIgnoreCase(Character key) {
			final CharacterNode<V> next = this.next(key);
			return next.isEmpty()
					? tryToggleCase(key).map(this::next).orElse(next)
					: next;
		}
	}

	public interface MutableCharacterNode
			<V, B extends MutableMapNode.Branch<Character, V, B> & MutableCharacterNode<V, B>>
			extends CharacterNode<V>, MutableMultiTrie.Node<Character, V> {
		@Override
		B next(Character key);

		@Override
		CharacterNode<V> view();

		@Override
		default MutableCharacterNode<V, B> nextIgnoreCase(Character key) {
			final MutableCharacterNode<V, B> next = this.next(key);
			return next.isEmpty()
					? tryToggleCase(key).<MutableCharacterNode<V, B>>map(this::next).orElse(next)
					: next;
		}
	}

	public abstract static class View
			<
				V,
				B extends MutableMapNode.Branch<Character, V, B> & MutableCharacterNode<V, B>,
				R extends MutableMapNode<Character, V, B> & MutableCharacterNode<V, B>
			>
			implements MultiTrie<Character, V> {
		@Override
		public CharacterNode<V> getRoot() {
			return this.getViewed().getRoot().view();
		}

		public CharacterNode<V> get(String prefix) {
			return this.getViewed().get(prefix).view();
		}

		public CharacterNode<V> getIgnoreCase(String prefix) {
			return this.getViewed().getIgnoreCase(prefix).view();
		}

		protected abstract StringMultiTrie<V, B, R> getViewed();
	}
}

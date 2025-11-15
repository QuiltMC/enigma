package org.quiltmc.enigma.util.multi_trie;

import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.BranchNode;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.MutableCharacterNode;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link MutableMultiTrie} that associates sequences of characters with values of type {@code V}.
 *
 * <p> Adds convenience methods for accessing contents by passing a {@link String} instead of passing individual
 * characters to nodes:
 * <ul>
 *     <li> {@link #get(String)}
 *     <li> {@link #put(String, Object)}
 *     <li> {@link #remove(String, Object)}
 *     <li> {@link #removeAll(String)}
 * </ul>
 *
 * <p> {@linkplain #getView() Views} also provide a {@link #get(String)} method.
 *
 * @param <V> the type of values
 * @param <B> the type of branch nodes
 */
public abstract class StringMultiTrie
		<
			V, B extends BranchNode<V, B> & MutableCharacterNode<V, B>,
			R extends MutableMapNode<Character, V, B> & MutableCharacterNode<V, B>
		>
		implements MutableMultiTrie<Character, V> {
	private static Optional<Character> tryToggleCase(char c) {
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
	public abstract View<V, B, R> getView();

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
		if (prefix.isEmpty()) {
			return node;
		}

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
			return root;
		}

		B node = root.next(string.charAt(0));
		for (int i = 1; i < string.length(); i++) {
			final B parent = node;
			final char key = string.charAt(i);
			node = node.getBranches().computeIfAbsent(key, ignored -> parent.createBranch(key));
		}

		node.put(value);

		return node;
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
		CharacterNode<V> getView();

		@Override
		default MutableCharacterNode<V, B> nextIgnoreCase(Character key) {
			final MutableCharacterNode<V, B> next = this.next(key);
			return next.isEmpty()
				? tryToggleCase(key).<MutableCharacterNode<V, B>>map(this::next).orElse(next)
				: next;
		}
	}

	public abstract static class BranchNode<V, B extends BranchNode<V, B>>
			extends MutableMapNode.Branch<Character, V, B>
			implements MutableCharacterNode<V, B> { }

	public abstract static class View
			<
				V, B extends BranchNode<V, B> & MutableCharacterNode<V, B>,
				R extends MutableMapNode<Character, V, B> & MutableCharacterNode<V, B>
			>
			implements MultiTrie<Character, V> {
		@Override
		public CharacterNode<V> getRoot() {
			return this.getViewed().getRoot().getView();
		}

		public CharacterNode<V> get(String prefix) {
			return this.getViewed().get(prefix).getView();
		}

		public CharacterNode<V> getIgnoreCase(String prefix) {
			return this.getViewed().getIgnoreCase(prefix).getView();
		}

		protected abstract StringMultiTrie<V, B, R> getViewed();
	}
}

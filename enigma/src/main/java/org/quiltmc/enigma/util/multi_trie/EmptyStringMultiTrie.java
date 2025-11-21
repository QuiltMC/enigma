package org.quiltmc.enigma.util.multi_trie;

import java.util.stream.Stream;

/**
 * An empty, immutable singleton {@link StringMultiTrie}.
 */
public final class EmptyStringMultiTrie<V> implements StringMultiTrie<V> {
	private static final EmptyStringMultiTrie<Object> INSTANCE = new EmptyStringMultiTrie<>();

	@SuppressWarnings("unchecked")
	public static <V> EmptyStringMultiTrie<V> get() {
		return (EmptyStringMultiTrie<V>) INSTANCE;
	}

	@Override
	public StringMultiTrie.Node<V> getRoot() {
		return Node.get();
	}

	@Override
	public StringMultiTrie.Node<V> get(String prefix) {
		return Node.get();
	}

	@Override
	public Stream<StringMultiTrie.Node<V>> streamIgnoreCase(String prefix) {
		return Stream.empty();
	}

	/**
	 * An empty, immutable singleton {@link StringMultiTrie.Node}.
	 */
	public static final class Node<V> implements StringMultiTrie.Node<V> {
		private static final Node<Object> INSTANCE = new Node<>();

		@SuppressWarnings("unchecked")
		public static <V> Node<V> get() {
			return (Node<V>) INSTANCE;
		}

		private Node() { }

		@Override
		public Stream<V> streamLeaves() {
			return Stream.empty();
		}

		@Override
		public Stream<V> streamStems() {
			return Stream.empty();
		}

		@Override
		public StringMultiTrie.Node<V> next(Character key) {
			return this;
		}

		@Override
		public Stream<StringMultiTrie.Node<V>> streamNextIgnoreCase(Character key) {
			return Stream.empty();
		}

		@Override
		public StringMultiTrie.Node<V> previous() {
			return this;
		}

		@Override
		public StringMultiTrie.Node<V> previous(int steps) {
			return this;
		}

		@Override
		public int getDepth() {
			return 0;
		}
	}
}

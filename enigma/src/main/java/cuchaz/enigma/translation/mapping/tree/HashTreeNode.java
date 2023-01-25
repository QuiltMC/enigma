package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashTreeNode<T> implements EntryTreeNode<T>, Iterable<HashTreeNode<T>> {
	private final Entry<?> entry;
	private final Map<Entry<?>, HashTreeNode<T>> children = new HashMap<>();
	private T value;

	HashTreeNode(Entry<?> entry) {
		this.entry = entry;
	}

	void putValue(T value) {
		this.value = value;
	}

	T removeValue() {
		T oldValue = this.value;
		this.value = null;
		return oldValue;
	}

	@Nullable
	HashTreeNode<T> getChild(Entry<?> entry) {
		return this.children.get(entry);
	}

	@Nonnull
	HashTreeNode<T> computeChild(Entry<?> entry) {
		return this.children.computeIfAbsent(entry, HashTreeNode::new);
	}

	void remove(Entry<?> entry) {
		this.children.remove(entry);
	}

	@Override
	@Nullable
	public T getValue() {
		return this.value;
	}

	@Override
	public Entry<?> getEntry() {
		return this.entry;
	}

	@Override
	public boolean isEmpty() {
		return this.children.isEmpty() && this.value == null;
	}

	@Override
	public Collection<Entry<?>> getChildren() {
		return this.children.keySet();
	}

	@Override
	public Collection<? extends EntryTreeNode<T>> getChildNodes() {
		return this.children.values();
	}

	@Override
	public Iterator<HashTreeNode<T>> iterator() {
		return this.children.values().iterator();
	}
}

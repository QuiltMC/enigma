package org.quiltmc.enigma.api.translation.mapping;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.stream.Stream;

public interface EntryMap<T> {
	void insert(Entry<?> entry, T value);

	default void insert(EntryTreeNode<T> node) {
		this.insert(node.getEntry(), node.getValue());
	}

	@Nullable
	T remove(Entry<?> entry);

	@Nullable
	T get(Entry<?> entry);

	default boolean contains(Entry<?> entry) {
		return this.get(entry) != null;
	}

	Stream<Entry<?>> getAllEntries();

	boolean isEmpty();
}

package org.quiltmc.enigma.analysis;

import org.quiltmc.enigma.translation.representation.entry.Entry;

public interface ReferenceTreeNode<E extends Entry<?>, C extends Entry<?>> {
	/**
	 * Returns the entry represented by this tree node.
	 */
	E getEntry();

	EntryReference<E, C> getReference();
}

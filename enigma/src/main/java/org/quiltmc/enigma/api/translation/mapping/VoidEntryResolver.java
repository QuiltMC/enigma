package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public enum VoidEntryResolver implements EntryResolver {
	INSTANCE;

	@Override
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		return Collections.singleton(entry);
	}

	@Override
	public Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		return Collections.singleton(entry);
	}

	@Override
	public Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		return Collections.singleton(methodEntry);
	}
}

package org.quiltmc.enigma.util;

import java.util.Set;

public class CombinedSet<T> extends CombinedCollection<T> implements Set<T> {
	public CombinedSet(Set<T> first, Set<T> second) {
		super(first, second);
	}
}

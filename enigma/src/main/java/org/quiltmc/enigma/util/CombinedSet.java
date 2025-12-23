package org.quiltmc.enigma.util;

import java.util.Set;

/**
 * A {@link CombinedCollection} that combines {@link Set}s.
 *
 * <p> <b>Note</b>: if backing sets share elements, those elements will appear twice in the combined set's
 * {@linkplain #iterator() iterators} and {@linkplain #stream() streams}.
 *
 * @param <T> the type of elements
 */
public class CombinedSet<T> extends CombinedCollection<T> implements Set<T> {
	public CombinedSet(Set<T> first, Set<T> second) {
		super(first, second);
	}
}

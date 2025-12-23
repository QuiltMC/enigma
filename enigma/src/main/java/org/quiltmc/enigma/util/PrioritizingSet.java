package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A set which distinguishes between and prioritizes equivalent values.
 *
 * <p> A {@link Comparator} determines priority; elements which are placed <em>earlier</em> have higher priority.<br>
 * Held values are replaced when an equivalent value with higher priority is {@linkplain #add(Object) added}.<br>
 * <b>Note</b>: {@link Comparator#compare(Object, Object)} returns negative numbers to indicate that the left
 * object comes first, so <em>low</em> comparisons indicate <em>high</em> (left) priority.
 *
 * @param <V> the type of values
 *
 * @see #add(Object)
 * @see #get(Object)
 * @see #addOrReplace(Object)
 */
public final class PrioritizingSet<V> implements Set<V> {
	private final Map<V, V> delegate;
	private final Supplier<Map<V, V>> delegateFactory;
	private final Comparator<V> prioritizer;

	/**
	 * Constructs a set backed by a map produced by the passed {@code delegateFactory} and prioritized by the passed
	 * {@code prioritizer}.
	 */
	public PrioritizingSet(Supplier<Map<V, V>> delegateFactory, Comparator<V> prioritizer) {
		this.delegateFactory = delegateFactory;
		this.delegate = delegateFactory.get();
		this.prioritizer = prioritizer;
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public boolean contains(Object o) {
		return this.delegate.containsKey(o);
	}

	/**
	 * @return a value equivalent to the passed object if such a value is held, or {@code null} otherwise;
	 * {@code null} may also be returned if the backing map supports {@code null} keys and the held value is
	 * {@code null}
	 */
	@SuppressWarnings("SuspiciousMethodCalls")
	public V get(Object o) {
		return this.delegate.get(o);
	}

	@Override
	public @NonNull Iterator<V> iterator() {
		return this.delegate.keySet().iterator();
	}

	@Override
	public Object @NonNull[] toArray() {
		return this.delegate.keySet().toArray();
	}

	@Override
	public <T> T @NonNull[] toArray(T @NonNull[] a) {
		return this.delegate.keySet().toArray(a);
	}

	/**
	 * Adds the passed {@code value} to this set if the set does not already contain an equivalent value,
	 * or if the passed {@code value} has higher priority than the current value.
	 *
	 * <p> <b>Note</b>: {@code true} may be returned even if the size of this set did not change.
	 *
	 * @return {@code true} if the contents of this set were updated, or {@code false} otherwise
	 *
	 * @see #addOrReplace(Object)
	 */
	@Override
	public boolean add(V value) {
		if (this.shouldAdd(value)) {
			this.put(value);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds the passed {@code value} to this set if the set does not already contain an equivalent value,
	 * or if the passed {@code value} has higher priority than the current value.
	 *
	 * @return any previously held value which was replaced because it was equivalent to the passed {@code value}
	 * but had lower priority if such a value was replaced, or {@code null} otherwise;<br>
	 * {@code null} may also be returned if the backing map supports {@code null} keys and the previously held value was
	 * {@code null}
	 */
	@Nullable
	public V addOrReplace(V value) {
		if (this.shouldAdd(value)) {
			return this.put(value);
		} else {
			return null;
		}
	}

	private boolean shouldAdd(V value) {
		return !this.contains(value) || this.currentComesAfter(value);
	}

	private boolean currentComesAfter(V value) {
		return this.prioritizer.compare(this.delegate.get(value), value) > 0;
	}

	private V put(V value) {
		return this.delegate.put(value, value);
	}

	@Override
	public boolean remove(Object o) {
		if (this.contains(o)) {
			this.delegate.remove(o);

			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean containsAll(@NonNull Collection<?> collection) {
		return this.delegate.keySet().containsAll(collection);
	}

	/**
	 * {@linkplain #add(Object) Adds} all of the values of the passed {@code collection} to this set.
	 *
	 * <p> <b>Note</b>: {@code true} may be returned even if the size of this set did not change.
	 *
	 * @return {@code true} if the contents of this were updated, or {@code false} otherwise
	 *
	 * @see #addOrReplaceAll(Collection)
	 * @see #add(Object)
	 */
	@Override
	public boolean addAll(@NonNull Collection<? extends V> collection) {
		boolean changed = false;
		for (final V value : collection) {
			changed |= this.add(value);
		}

		return changed;
	}

	/**
	 * {@linkplain #addOrReplace(Object) Adds} all values of the passed {@code collection} to this set.
	 *
	 * @return a new set containing any replaced values;
	 * the returned set has the same {@link #delegateFactory} and {@link #prioritizer} as this one
	 *
	 * @see #addAll(Collection)
	 * @see #addOrReplace(Object)
	 */
	public PrioritizingSet<V> addOrReplaceAll(@NonNull Collection<? extends V> collection) {
		final PrioritizingSet<V> replacements = new PrioritizingSet<>(this.delegateFactory, this.prioritizer);
		for (final V value : collection) {
			if (this.contains(value)) {
				if (this.currentComesAfter(value)) {
					replacements.add(value);
				}
			} else {
				this.put(value);
			}
		}

		for (final V replacement : replacements) {
			this.put(replacement);
		}

		return replacements;
	}

	@Override
	public boolean retainAll(@NonNull Collection<?> collection) {
		return this.delegate.keySet().retainAll(collection);
	}

	@Override
	public boolean removeAll(@NonNull Collection<?> collection) {
		return this.delegate.keySet().removeAll(collection);
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}
}

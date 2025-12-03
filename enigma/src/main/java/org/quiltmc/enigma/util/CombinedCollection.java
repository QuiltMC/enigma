package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A collection backed by two other collections.
 *
 * <p> Does <b>not</b> support the {@link #add(Object)} or {@link #addAll(Collection)} methods!
 *
 * <p> Removal is supported.
 *
 * @param <T> the type of elements
 */
public class CombinedCollection<T> implements Collection<T> {
	private static UnsupportedOperationException createUnsupportedAddException() {
		return new UnsupportedOperationException("cannot add to combined collection!");
	}

	private final Collection<T> first;
	private final Collection<T> second;

	public CombinedCollection(Collection<T> first, Collection<T> second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public int size() {
		return this.first.size() + this.second.size();
	}

	@Override
	public boolean isEmpty() {
		return this.first.isEmpty() && this.second.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.first.contains(o) || this.second.contains(o);
	}

	@NonNull
	@Override
	public Iterator<T> iterator() {
		return new CombinedIterator();
	}

	@Override
	public Object @NonNull[] toArray() {
		final int size = this.size();
		final Object[] array = new Object[size];
		final Iterator<T> itr = this.iterator();
		int i = 0;
		while (itr.hasNext()) {
			array[i++] = itr.next();
		}

		return array;
	}

	// Based on HashMap::prepareArray and HashMap::keysToArray
	@SuppressWarnings("unchecked")
	@Override
	public <T1> T1 @NonNull[] toArray(T1[] a) {
		final int size = this.size();
		if (a.length < size) {
			a = (T1[]) java.lang.reflect.Array
				.newInstance(a.getClass().getComponentType(), size);
		} else if (a.length > size) {
			a[size] = null;
		}

		final Iterator<T> itr = this.iterator();
		final Object[] objects = a;
		for (int i = 0; i < size; i++) {
			objects[i] = itr.next();
		}

		return a;
	}

	@Override
	public boolean add(T t) {
		throw createUnsupportedAddException();
	}

	@Override
	public boolean remove(Object o) {
		return this.first.remove(o) | this.second.remove(o);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public boolean containsAll(@NonNull Collection<?> collection) {
		return collection.stream().allMatch(o -> this.first.contains(o) || this.second.contains(o));
	}

	@Override
	public boolean addAll(@NonNull Collection<? extends T> collection) {
		throw createUnsupportedAddException();
	}

	@Override
	public boolean retainAll(@NonNull Collection<?> collection) {
		return this.first.retainAll(collection) | this.second.retainAll(collection);
	}

	@Override
	public boolean removeAll(@NonNull Collection<?> collection) {
		return this.first.removeAll(collection) | this.second.removeAll(collection);
	}

	@Override
	public void clear() {
		this.first.clear();
		this.second.clear();
	}

	private class CombinedIterator implements Iterator<T> {
		Iterator<T> delegate = CombinedCollection.this.first.iterator();

		boolean iteratingFirst = true;

		@Override
		public boolean hasNext() {
			final boolean currentHasNext = this.delegate.hasNext();
			if (currentHasNext) {
				return true;
			} else {
				if (this.iteratingFirst) {
					this.delegate = CombinedCollection.this.second.iterator();
					this.iteratingFirst = false;

					return this.delegate.hasNext();
				} else {
					return false;
				}
			}
		}

		@Override
		public T next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}

			return this.delegate.next();
		}

		@SuppressWarnings("ResultOfMethodCallIgnored")
		@Override
		public void remove() {
			// update delegate
			this.hasNext();
			this.delegate.remove();
		}
	}
}

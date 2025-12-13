package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;

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
		final Iterator<T> first = CombinedCollection.this.first.iterator();
		final Iterator<T> second = CombinedCollection.this.second.iterator();

		// null iff next has not been called
		@Nullable
		Iterator<T> nextSource;

		@Override
		public boolean hasNext() {
			return this.first.hasNext() || this.second.hasNext();
		}

		@Override
		public T next() {
			this.nextSource = this.first.hasNext() ? this.first : this.second;
			return this.nextSource.next();
		}

		@Override
		public void remove() {
			if (this.nextSource == null) {
				throw new IllegalStateException("remove called before any calls to next!");
			} else {
				this.nextSource.remove();
			}
		}
	}
}

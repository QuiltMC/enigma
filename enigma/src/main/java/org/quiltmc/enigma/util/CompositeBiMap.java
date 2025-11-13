package org.quiltmc.enigma.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.MapMaker;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CompositeBiMap<K, V> implements BiMap<K, V> {
	public static <K, V> BiMap<K, V> ofWeakValues() {
		return of(new MapMaker().weakValues().makeMap(), new MapMaker().weakKeys().makeMap());
	}

	public static <K, V> BiMap<K, V> of(Map<K, V> forward, Map<V, K> reverse) {
		return new CompositeBiMap<>(forward, reverse);
	}

	private final Map<K, V> forward;
	private final Map<V, K> reverse;

	CompositeBiMap<V, K> inverse;

	private CompositeBiMap(Map<K, V> forward, Map<V, K> reverse) {
		this.forward = forward;
		this.reverse = reverse;
	}

	@Override
	public int size() {
		return this.forward.size();
	}

	@Override
	public boolean isEmpty() {
		return this.forward.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.forward.containsKey(key);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public boolean containsValue(Object value) {
		return this.reverse.containsKey(value);
	}

	@Override
	public V get(Object key) {
		return this.forward.get(key);
	}

	/**
	 * @throws IllegalArgumentException see {@link BiMap#put(Object, Object)}
	 *
	 * @see #put(Object, Object)
	 */
	@CheckForNull
	@Override
	public V put(K key, V value) {
		if (this.containsValue(value)) {
			throw new IllegalArgumentException(
				"Tried to put duplicate value %s, already associated with key %s!"
					.formatted(value, this.reverse.get(value))
			);
		} else {
			return this.forcePut(key, value);
		}
	}

	@Override
	public V remove(Object key) {
		final V removed = this.forward.remove(key);
		if (removed == null) {
			return null;
		} else {
			this.reverse.remove(removed);
			return removed;
		}
	}

	@CheckForNull
	@Override
	public V forcePut(K key, V value) {
		this.reverse.put(value, key);
		return this.forward.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		this.forward.clear();
		this.reverse.clear();
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	@Nonnull
	public Set<K> keySet() {
		return new LiveSet<>(Map.Entry::getKey, "key", this.forward::keySet, this.forward::containsKey);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	@Nonnull
	public Set<V> values() {
		return new LiveSet<>(Map.Entry::getValue, "value", this.reverse::keySet, this.reverse::containsKey);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	@Nonnull
	public Set<Entry<K, V>> entrySet() {
		return new LiveSet<>(
				Function.identity(), "entry", this.forward::entrySet,
				o -> o instanceof Map.Entry<?, ?> e
					&& this.forward.containsKey(e.getKey())
					&& this.reverse.containsKey(e.getValue())
		);
	}

	@Override
	@Nonnull
	public BiMap<V, K> inverse() {
		if (this.inverse == null) {
			this.inverse = new Inverse<>(this);
		}

		return this.inverse;
	}

	private static class Inverse<V, K> extends CompositeBiMap<V, K> {
		Inverse(CompositeBiMap<K, V> original) {
			super(original.reverse, original.forward);
			this.inverse = original;
		}

		@Override
		@Nonnull
		public BiMap<K, V> inverse() {
			return this.inverse;
		}
	}

	private class LiveSet<E> implements Set<E> {
		private static UnsupportedOperationException createAddException(String elementName) {
			return new UnsupportedOperationException("Cannot add to map via " + elementName + " set!");
		}

		final Function<Map.Entry<K, V>, E> elementFromEntry;
		final Supplier<Set<E>> getDelegateSet;
		final Predicate<Object> containsElement;

		final String elementName;

		LiveSet(
				Function<Entry<K, V>, E> elementFromEntry, String elementName,
				Supplier<Set<E>> getDelegateSet, Predicate<Object> containsElement
		) {
			this.elementFromEntry = elementFromEntry;
			this.elementName = elementName;
			this.getDelegateSet = getDelegateSet;
			this.containsElement = containsElement;
		}

		@Override
		public int size() {
			return CompositeBiMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return CompositeBiMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return this.containsElement.test(o);
		}

		@Override
		@Nonnull
		public Iterator<E> iterator() {
			return new LiveIterator();
		}

		@Override
		@Nonnull
		public Object[] toArray() {
			return this.getDelegateSet.get().toArray();
		}

		@Override
		@Nonnull
		public <T> T[] toArray(@Nonnull T[] array) {
			return this.getDelegateSet.get().toArray(array);
		}

		@Override
		public boolean add(E element) {
			throw createAddException(this.elementName);
		}

		@Override
		public boolean remove(Object o) {
			return CompositeBiMap.this.remove(o) != null;
		}

		@Override
		public boolean containsAll(@Nonnull Collection<?> collection) {
			for (final Object o : collection) {
				if (!this.containsElement.test(o)) {
					return false;
				}
			}

			return true;
		}

		@Override
		public boolean addAll(@Nonnull Collection<? extends E> collection) {
			throw createAddException(this.elementName);
		}

		@Override
		public boolean retainAll(@Nonnull Collection<?> collection) {
			return this.removeAllMatching(key -> !collection.contains(key));
		}

		@Override
		public boolean removeAll(Collection<?> collection) {
			return this.removeAllMatching(collection::contains);
		}

		private boolean removeAllMatching(Predicate<E> predicate) {
			boolean removed = false;
			final Iterator<Entry<K, V>> itr = CompositeBiMap.this.forward.entrySet().iterator();
			while (itr.hasNext()) {
				final Entry<K, V> entry = itr.next();
				if (predicate.test(this.elementFromEntry.apply(entry))) {
					removed = true;
					itr.remove();
					CompositeBiMap.this.reverse.remove(entry.getValue());
				}
			}

			return removed;
		}

		@Override
		public void clear() {
			CompositeBiMap.this.clear();
		}

		private final class LiveIterator implements Iterator<E> {
			final Iterator<Entry<K, V>> delegate = CompositeBiMap.this.forward.entrySet().iterator();

			Entry<K, V> current;

			@Override
			public boolean hasNext() {
				return this.delegate.hasNext();
			}

			@Override
			public E next() {
				this.current = this.delegate.next();
				return LiveSet.this.elementFromEntry.apply(this.current);
			}

			@Override
			public void remove() {
				this.delegate.remove();

				CompositeBiMap.this.reverse.remove(this.current.getValue());
			}
		}
	}
}

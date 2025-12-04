package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * An unmodifiable view of two backing maps.
 *
 * <p> <b>Only intended for maps no {@link #keySet()} overlap.</b> Behavior is undefined in the case of overlap.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class UnmodifiableCombinedMap<K, V> implements Map<K, V> {
	private static UnsupportedOperationException createUnsupportedModificationException() {
		return new UnsupportedOperationException("cannot modify combined map!");
	}

	private final Map<K, V> first;
	private final Map<K, V> second;

	private final Set<K> keySet;
	private final Collection<V> values;
	private final Set<Entry<K, V>> entrySet;

	public UnmodifiableCombinedMap(Map<K, V> first, Map<K, V> second) {
		this.first = first;
		this.second = second;

		this.keySet = Collections.unmodifiableSet(new CombinedSet<>(this.first.keySet(), this.second.keySet()));
		this.values = Collections
			.unmodifiableCollection(new CombinedCollection<>(this.first.values(), this.second.values()));
		this.entrySet = Collections.unmodifiableSet(new CombinedSet<>(this.first.entrySet(), this.second.entrySet()));
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
	public boolean containsKey(Object key) {
		return this.first.containsKey(key) || this.second.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.first.containsValue(value) || this.second.containsValue(value);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public V get(Object key) {
		return this.first.containsKey(key) ? this.first.get(key) : this.second.get(key);
	}

	@Override
	public V put(K key, V value) {
		throw createUnsupportedModificationException();
	}

	@Override
	public V remove(Object key) {
		throw createUnsupportedModificationException();
	}

	@Override
	public void putAll(@NonNull Map<? extends K, ? extends V> m) {
		throw createUnsupportedModificationException();
	}

	@Override
	public void clear() {
		throw createUnsupportedModificationException();
	}

	@NonNull
	@Override
	public Set<K> keySet() {
		return this.keySet;
	}

	@NonNull
	@Override
	public Collection<V> values() {
		return this.values;
	}

	@NonNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.entrySet;
	}
}

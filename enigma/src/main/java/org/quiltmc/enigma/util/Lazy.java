package org.quiltmc.enigma.util;

import java.util.function.Supplier;

/**
 * A lazily populated value.
 *
 * <p> {@link Simple Simple} implementations are populated by {@link Supplier}s.<br>
 * Simple implementations' {@link #get()} methods return {@code null} <em>iff</em>
 * their {@link Supplier} returns {@code null}.<br>
 * They do not attempt to re-populate if their suppliers return {@code null}.
 *
 * <p> The following factory methods return {@link Simple Simple} implementations:
 * <ul>
 *     <li> {@link #of(Supplier)}
 *     <li> {@link #clearableOf(Supplier)}
 * </ul>
 *
 * @param <V> the value type
 */
public interface Lazy<V> {
	static <V> Lazy<V> of(Supplier<V> supplier) {
		return new Simple<>(supplier);
	}

	static <V> Clearable<V> clearableOf(Supplier<V> supplier) {
		return new Clearable.Simple<>(supplier);
	}

	V get();

	/**
	 * A {@link Lazy} value which may be {@linkplain #clear() cleared}.<br>
	 * Cleared values are re-populated as needed.
	 *
	 * @param <V> the value type
	 */
	interface Clearable<V> extends Lazy<V> {
		void clear();

		class Simple<V> extends Lazy.Simple<V> implements Clearable<V> {
			protected final Supplier<V> supplier;

			public Simple(Supplier<V> supplier) {
				super(supplier);
				this.supplier = supplier;
			}

			@Override
			public void clear() {
				this.populated = false;
				this.value = null;
			}
		}
	}

	class Simple<V> implements Lazy<V> {
		protected final Supplier<V> supplier;

		protected boolean populated;
		protected V value;

		public Simple(Supplier<V> supplier) {
			this.supplier = supplier;
		}

		@Override
		public V get() {
			if (!this.populated) {
				this.value = this.supplier.get();
				this.populated = true;
			}

			return this.value;
		}
	}
}

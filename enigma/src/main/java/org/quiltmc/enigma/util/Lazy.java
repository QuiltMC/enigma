package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;

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
				this.value = Either.left(this.supplier);
			}
		}
	}

	class Simple<V> implements Lazy<V> {
		@NonNull
		protected Either<Supplier<V>, V> value;

		public Simple(Supplier<V> supplier) {
			this.value = Either.left(supplier);
		}

		@Override
		public V get() {
			if (this.value.isLeft()) {
				this.value = Either.right(this.value.leftOrThrow().get());
			}

			return this.value.rightOrThrow();
		}
	}
}

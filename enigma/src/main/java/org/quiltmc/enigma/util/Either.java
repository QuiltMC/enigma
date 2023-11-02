package org.quiltmc.enigma.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@JsonAdapter(Either.CustomTypeAdapterFactory.class)
public abstract class Either<L, R> {
	public abstract <T> T map(Function<L, ? extends T> l, Function<R, ? extends T> r);

	public <A, B> Either<? extends A, ? extends B> flatMap(Function<L, Either<? extends A, ? extends B>> l, Function<R, Either<? extends A, ? extends B>> r) {
		return this.map(l, r);
	}

	public abstract <A, B> Either<A, B> mapBoth(Function<L, ? extends A> l, Function<R, ? extends B> r);

	public <T> Either<T, R> mapLeft(Function<L, ? extends T> l) {
		return this.map(v -> left(l.apply(v)), Either::right);
	}

	public <T> Either<L, T> mapRight(Function<R, ? extends T> r) {
		return this.map(Either::left, v -> right(r.apply(v)));
	}

	public abstract Either<L, R> ifLeft(Consumer<? super L> consumer);

	public abstract Either<L, R> ifRight(Consumer<? super R> consumer);

	public abstract boolean isLeft();

	public abstract boolean isRight();

	public abstract Optional<L> left();

	public abstract Optional<R> right();

	public L leftOrThrow() {
		return this.left().orElseThrow();
	}

	public R rightOrThrow() {
		return this.right().orElseThrow();
	}

	public static <L, R> Either<L, R> left(L value) {
		return new Left<>(value);
	}

	public static <L, R> Either<L, R> right(R value) {
		return new Right<>(value);
	}

	private static final class Left<L, R> extends Either<L, R> {
		private final L value;

		private Left(L value) {
			this.value = value;
		}

		@Override
		public <T> T map(Function<L, ? extends T> l, Function<R, ? extends T> r) {
			return l.apply(this.value);
		}

		@Override
		public <A, B> Either<A, B> mapBoth(Function<L, ? extends A> l, Function<R, ? extends B> r) {
			return new Left<>(l.apply(this.value));
		}

		@Override
		public Either<L, R> ifLeft(Consumer<? super L> consumer) {
			consumer.accept(this.value);
			return this;
		}

		@Override
		public Either<L, R> ifRight(Consumer<? super R> consumer) {
			return this;
		}

		@Override
		public boolean isLeft() {
			return true;
		}

		@Override
		public boolean isRight() {
			return false;
		}

		@Override
		public Optional<L> left() {
			return Optional.of(this.value);
		}

		@Override
		public Optional<R> right() {
			return Optional.empty();
		}
	}

	private static final class Right<L, R> extends Either<L, R> {
		private final R value;

		private Right(R value) {
			this.value = value;
		}

		@Override
		public <T> T map(Function<L, ? extends T> l, Function<R, ? extends T> r) {
			return r.apply(this.value);
		}

		@Override
		public <A, B> Either<A, B> mapBoth(Function<L, ? extends A> l, Function<R, ? extends B> r) {
			return new Right<>(r.apply(this.value));
		}

		@Override
		public Either<L, R> ifLeft(Consumer<? super L> consumer) {
			return this;
		}

		@Override
		public Either<L, R> ifRight(Consumer<? super R> consumer) {
			consumer.accept(this.value);
			return this;
		}

		@Override
		public boolean isLeft() {
			return false;
		}

		@Override
		public boolean isRight() {
			return true;
		}

		@Override
		public Optional<L> left() {
			return Optional.empty();
		}

		@Override
		public Optional<R> right() {
			return Optional.of(this.value);
		}
	}

	static final class CustomTypeAdapterFactory implements TypeAdapterFactory {
		@SuppressWarnings("unchecked")
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
			Type type = typeToken.getType();
			if (typeToken.getRawType() != Either.class || !(type instanceof ParameterizedType parameterizedType)) {
				return null;
			}

			Type leftType = parameterizedType.getActualTypeArguments()[0];
			Type rightType = parameterizedType.getActualTypeArguments()[1];
			TypeAdapter<?> leftAdapter = gson.getAdapter(TypeToken.get(leftType));
			TypeAdapter<?> rightAdapter = gson.getAdapter(TypeToken.get(rightType));

			return (TypeAdapter<T>) this.newEitherAdapter(leftAdapter, rightAdapter);
		}

		private <L, R> TypeAdapter<Either<L, R>> newEitherAdapter(TypeAdapter<L> leftAdapter, TypeAdapter<R> rightAdapter) {
			return new TypeAdapter<>() {
				@Override
				public void write(JsonWriter out, Either<L, R> value) throws IOException {
					if (value.isLeft()) {
						leftAdapter.write(out, value.leftOrThrow());
					} else {
						rightAdapter.write(out, value.rightOrThrow());
					}
				}

				@Override
				public Either<L, R> read(JsonReader in) throws IOException {
					try {
						L left = leftAdapter.read(in);
						return left(left);
					} catch (JsonParseException | IllegalStateException ignored) {
						// ignore
					}

					try {
						R right = rightAdapter.read(in);
						return right(right);
					} catch (JsonParseException | IllegalStateException ignored) {
						// ignore
					}

					return null;
				}
			};
		}
	}
}

package org.quiltmc.enigma.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T, E> {
	static <T, E> Result<T, E> ok(T ok) {
		return new Ok<>(ok);
	}

	static <T, E> Result<T, E> err(E err) {
		return new Err<>(err);
	}

	boolean isOk();

	boolean isErr();

	Optional<T> ok();

	Optional<E> err();

	T unwrap();

	E unwrapErr();

	T unwrapOr(T fallback);

	T unwrapOrElse(Function<E, T> fallback);

	<U> Result<U, E> map(Function<T, U> mapper);

	<F> Result<T, F> mapErr(Function<E, F> mapper);

	<U> Result<U, E> and(Result<U, E> next);

	<U> Result<U, E> andThen(Function<T, Result<U, E>> op);

	record Ok<T, E>(T value) implements Result<T, E> {
		public Ok {
			Objects.requireNonNull(value);
		}

		@Override
		public boolean isOk() {
			return true;
		}

		@Override
		public boolean isErr() {
			return false;
		}

		@Override
		public Optional<T> ok() {
			return Optional.of(this.value);
		}

		@Override
		public Optional<E> err() {
			return Optional.empty();
		}

		@Override
		public T unwrap() {
			return this.value;
		}

		@Override
		public E unwrapErr() {
			throw new IllegalStateException(String.format("Called Result.unwrapErr on an Ok value: %s", this.value));
		}

		@Override
		public T unwrapOr(T fallback) {
			return this.value;
		}

		@Override
		public T unwrapOrElse(Function<E, T> fallback) {
			return this.value;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Result<U, E> map(Function<T, U> mapper) {
			return (Result<U, E>) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <F> Result<T, F> mapErr(Function<E, F> mapper) {
			return (Result<T, F>) this;
		}

		@Override
		public <U> Result<U, E> and(Result<U, E> next) {
			return next;
		}

		@Override
		public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
			return op.apply(this.value);
		}

		@Override
		public String toString() {
			return String.format("Result.Ok(%s)", this.value);
		}
	}

	record Err<T, E>(E value) implements Result<T, E> {
		public Err {
			Objects.requireNonNull(value);
		}

		@Override
		public boolean isOk() {
			return false;
		}

		@Override
		public boolean isErr() {
			return true;
		}

		@Override
		public Optional<T> ok() {
			return Optional.empty();
		}

		@Override
		public Optional<E> err() {
			return Optional.of(this.value);
		}

		@Override
		public T unwrap() {
			throw new IllegalStateException(String.format("Called Result.unwrap on an Err value: %s", this.value));
		}

		@Override
		public E unwrapErr() {
			return this.value;
		}

		@Override
		public T unwrapOr(T fallback) {
			return fallback;
		}

		@Override
		public T unwrapOrElse(Function<E, T> fallback) {
			return fallback.apply(this.value);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Result<U, E> map(Function<T, U> mapper) {
			return (Result<U, E>) this;
		}

		@Override
		public <F> Result<T, F> mapErr(Function<E, F> mapper) {
			return Result.err(mapper.apply(this.value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Result<U, E> and(Result<U, E> next) {
			return (Result<U, E>) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
			return (Result<U, E>) this;
		}

		@Override
		public String toString() {
			return String.format("Result.Err(%s)", this.value);
		}
	}
}

package org.quiltmc.enigma.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

record OkResult<T, E>(T value) implements Result<T, E> {
	public OkResult {
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
	public <U> Result<U, E> map(Function<T, U> mapper) {
		return Result.ok(mapper.apply(this.value));
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
	public <U> Result<U, E> andThen(Function<T, Result<U, E>> next) {
		return next.apply(this.value);
	}
}

package org.quiltmc.enigma.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

record ErrResult<T, E>(E error) implements Result<T, E> {
	public ErrResult {
		Objects.requireNonNull(error);
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
		return Optional.of(this.error);
	}

	@Override
	public T unwrap() {
		throw new IllegalStateException(String.format("Called Result.unwrap on an Err value: %s", this.error));
	}

	@Override
	public E unwrapErr() {
		return this.error;
	}

	@Override
	public T unwrapOr(T fallback) {
		return fallback;
	}

	@Override
	public T unwrapOrElse(Function<E, T> fallback) {
		return fallback.apply(this.error);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> Result<U, E> map(Function<T, U> mapper) {
		return (Result<U, E>) this;
	}

	@Override
	public <F> Result<T, F> mapErr(Function<E, F> mapper) {
		return Result.err(mapper.apply(this.error));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> Result<U, E> and(Result<U, E> next) {
		return (Result<U, E>) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> Result<U, E> andThen(Function<T, Result<U, E>> next) {
		return (Result<U, E>) this;
	}
}

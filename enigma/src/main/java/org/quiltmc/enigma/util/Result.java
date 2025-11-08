package org.quiltmc.enigma.util;

import java.util.Optional;
import java.util.function.Function;

/**
 * Represents the result of an operation which may or may not have been successful.
 *
 * <p> A result holds either a value of type {@code T} which is the result of a successful operation,
 * or an error of type {@code E} which is the result of a failed operation.
 *
 * @param <T> the value type held by a successful result
 * @param <E> the error type held by an unsuccessful result
 */
public sealed interface Result<T, E> permits OkResult, ErrResult {
	/**
	 * Creates a successful result holding the passed {@code value}.
	 *
	 * @param value the value held by the returned result; must not be {@code null}
	 *
	 * @param <T> the value type held by a successful result
	 * @param <E> the error type held by an unsuccessful result
	 *
	 * @return a successful result holding the passed {@code value}
	 *
	 * @throws NullPointerException if the passed {@code value} is {@code null}
	 */
	static <T, E> Result<T, E> ok(T value) {
		return new OkResult<>(value);
	}

	/**
	 * Creates an error result holding the passed {@code error}.
	 *
	 * @param error the error held by the returned result; must not be {@code null}
	 *
	 * @param <T> the value type held by a successful result
	 * @param <E> the error type held by an unsuccessful result
	 *
	 * @return an error result holding the passed {@code error}
	 *
	 * @throws NullPointerException if the passed {@code error} is {@code null}
	 */
	static <T, E> Result<T, E> err(E error) {
		return new ErrResult<>(error);
	}

	/**
	 * @return {@code true} if this result is a success, or {@code false} otherwise;
	 * always the inverse of {@link #isErr()}
	 */
	boolean isOk();

	/**
	 * @return {@code true} if this result is an error, or {@code false otherwise};
	 * always the inverse of {@link #isOk()}
	 */
	boolean isErr();

	/**
	 * @return an {@link Optional} holding this result's value if this result is a success,
	 * or {@link Optional#empty()} otherwise
	 */
	Optional<T> ok();

	/**
	 * @return an {@link Optional} holding this result's error if this result is an error,
	 * or {@link Optional#empty()} otherwise
	 */
	Optional<E> err();

	/**
	 * @return the result of a {@linkplain #isOk() successful} operation
	 *
	 * @throws IllegalStateException if this result {@linkplain #isErr() is an error}
	 */
	T unwrap();

	/**
	 * @return the error of an {@linkplain #isErr() unsuccessful} operation
	 *
	 * @throws IllegalStateException if this result {@linkplain #isOk() is a success}
	 */
	E unwrapErr();

	/**
	 * @return this result's value if this result {@linkplain #isOk() is a success},
	 * or the passed {@code fallback} otherwise
	 */
	T unwrapOr(T fallback);

	/**
	 * @param fallback a function that supplies a value to return if this result is an error
	 *
	 * @return this result's value if this result {@linkplain #isOk() is a success},
	 * or the return value of the passed {@code fallback} function applied to this result's error otherwise
	 */
	T unwrapOrElse(Function<E, T> fallback);

	/**
	 * @param mapper a function that generates a value from this result's value if this result is a success
	 *
	 * @return the result of applying the passed {@code mapper} to this result's value if this result is a success,
	 * or this result if this is an error
	 *
	 * @param <U> the value type held by a returned successful result
	 */
	<U> Result<U, E> map(Function<T, U> mapper);

	/**
	 * @param mapper a function that creates a value from this result's error if this result is an error
	 *
	 * @return the result of applying the passed {@code mapper} to this result's value if this result is an error,
	 * or this result if this is a success
	 *
	 * @param <F> the error type held by a returned error result
	 */
	<F> Result<T, F> mapErr(Function<E, F> mapper);

	/**
	 * @param next another result, to be returned if this result is a success
	 *
	 * @return the passed {@code next} result of this result is a success, or this result otherwise
	 *
	 * @param <U> the value type held by a returned successful result
	 */
	<U> Result<U, E> and(Result<U, E> next);

	/**
	 * @param next a function that generates a new result from this result's value if this result is a success
	 *
	 * @return the result created by applying the passed {@code next} function to this result's value
	 * if this result is a success, or this result otherwise
	 *
	 * @param <U> the value type held by a returned successful result
	 */
	<U> Result<U, E> andThen(Function<T, Result<U, E>> next);
}

package org.quiltmc.enigma.api.translation;

import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the result of a translation operation on an arbitrary object.
 * @param <T> the type of the translated object
 */
public final class TranslateResult<T extends Translatable> {
	private final EntryMapping mapping;
	private final T value;

	private TranslateResult(EntryMapping mapping, T value) {
		this.mapping = mapping;
		this.value = value;
	}

	/**
	 * Creates a translation result for the given value.
	 * @param mapping the value's mapping
	 * @param value the translated value
	 * @return a result containing that value
	 */
	public static <T extends Translatable> TranslateResult<T> of(EntryMapping mapping, T value) {
		Objects.requireNonNull(mapping, "mapping must not be null");
		return new TranslateResult<>(mapping, value);
	}

	/**
	 * Creates a translation result for values that cannot have an attached mapping, such as method descriptors.
	 * @param value the translated value
	 * @return a result containing that value
	 */
	public static <T extends Translatable> TranslateResult<T> ungrouped(T value) {
		return TranslateResult.obfuscated(value);
	}

	/**
	 * Creates an obfuscated translation result.
	 * @param value the obfuscated value
	 * @return a result containing that value
	 */
	public static <T extends Translatable> TranslateResult<T> obfuscated(T value) {
		return TranslateResult.of(EntryMapping.OBFUSCATED, value);
	}

	/**
	 * {@return the token type of the result mapping}
	 */
	public TokenType getType() {
		return this.mapping.tokenType();
	}

	/**
	 * {@return the result mapping}
	 */
	public EntryMapping getMapping() {
		return this.mapping;
	}

	/**
	 * {@return the translated value}
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * {@return whether this result is obfuscated}
	 */
	public boolean isObfuscated() {
		return this.getType() == TokenType.OBFUSCATED;
	}

	/**
	 * {@return whether this result is manually mapped}
	 *
	 * @deprecated for removal in 3.0.0. This method's name is misleading, as it does not check that the mapping is not obfuscated, it checks whether the token type is equal to
	 * {@link TokenType#DEOBFUSCATED}, which equates to a manual mapping. For the old behaviour, you can manually compare {@link #getType()} with {@link TokenType#DEOBFUSCATED}.
	 * In order to check that the mapping is not obfuscated, use {@code !}{@link #isObfuscated()}.
	 */
	@Deprecated(forRemoval = true, since = "2.6.0")
	public boolean isDeobfuscated() {
		return this.getType() == TokenType.DEOBFUSCATED;
	}

	/**
	 * Creates a new result, applying {@code op} to the {@link #value} without changing the {@link #mapping}.
	 * @param op the operation to apply to the current value
	 * @return the new result
	 */
	public <R extends Translatable> TranslateResult<R> map(Function<T, R> op) {
		return TranslateResult.of(this.mapping, op.apply(this.value));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		TranslateResult<?> that = (TranslateResult<?>) o;
		return Objects.equals(this.mapping, that.mapping) && Objects.equals(this.value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.mapping, this.value);
	}

	@Override
	public String toString() {
		return String.format("TranslateResult { mapping: %s, value: %s }", this.mapping, this.value);
	}
}

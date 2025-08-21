package org.quiltmc.enigma.command;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PredicateParserTest {
	private static final PredicateParser<String, String> CONTAINS_COMBO = new PredicateParser<>(
			Function.identity(), (string, tested) -> tested.contains(string)
	);

	@ParameterizedTest
	@MethodSource("streamValidCases")
	void testValid(ValidCase validCase) {
		final Predicate<String> predicate = CONTAINS_COMBO.parsePredicate(validCase.combo);
		for (final String expected : validCase.expected) {
			assertTrue(
					predicate.test(expected),
					() -> "Expected combo '%s' to match '%s'!".formatted(validCase.combo, expected)
			);
		}

		for (final String unexpected : validCase.unexpected) {
			assertFalse(
					predicate.test(unexpected),
					() -> "Did not expect combo '%s' to match '%s'!".formatted(validCase.combo, unexpected)
			);
		}
	}

	private static Stream<ValidCase> streamValidCases() {
		return Stream
				.of(
					validBuilder("a").expect("a", "art", "pita", "zap", "data").unexpect("", "b", "xyz"),
					validBuilder("a&b").expect("lab", "beard").unexpect("xyz", "as", "orbit"),
					validBuilder("(a&b)|(e&f)").expect("lab", "effect").unexpect("xyz", "ae", "af", "be", "bf")
				)
				.map(ValidCase.Builder::build);
	}

	private static ValidCase.Builder validBuilder(String combo) {
		return new ValidCase.Builder(combo);
	}

	record ValidCase(String combo, Set<String> expected, Set<String> unexpected) {
		static class Builder {
			final String combo;
			@Nullable
			Set<String> expect;
			@Nullable
			Set<String> unexpect;

			Builder(String combo) {
				if (combo == null || combo.isEmpty()) {
					throw new IllegalArgumentException("Combo must not be empty!");
				}

				this.combo = combo;
			}

			Builder expect(String... expect) {
				this.expect = Set.of(expect);
				return this;
			}

			Builder unexpect(String... unexpect) {
				this.unexpect = Set.of(unexpect);
				return this;
			}

			ValidCase build() {
				if (this.expect == null || this.expect.isEmpty()) {
					throw new IllegalStateException("Expect must not be empty for combo '%s'!".formatted(this.combo));
				}

				if (this.unexpect == null || this.unexpect.isEmpty()) {
					throw new IllegalStateException("Unexpect must not be empty for combo '%s'!".formatted(this.combo));
				}

				return new ValidCase(this.combo, this.expect, this.unexpect);
			}
		}
	}
}

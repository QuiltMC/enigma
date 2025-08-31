package org.quiltmc.enigma.command;

import com.google.common.collect.Streams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quiltmc.enigma.command.PredicateParser.AND;
import static org.quiltmc.enigma.command.PredicateParser.CLOSE;
import static org.quiltmc.enigma.command.PredicateParser.NOT;
import static org.quiltmc.enigma.command.PredicateParser.OPEN;
import static org.quiltmc.enigma.command.PredicateParser.OR;

public class PredicateParserTest {
	private static final PredicateParser<String, String> CONTAINS_PARSER = new PredicateParser<>(
			Function.identity(), (string, tested) -> tested.contains(string)
	);

	@ParameterizedTest
	@MethodSource("streamValidCases")
	void testValid(ValidCase valid) {
		final Predicate<String> predicate = CONTAINS_PARSER.parse(valid.predicate);
		testValidImpl(valid, predicate);
	}

	@ParameterizedTest
	@MethodSource("streamValidCases")
	void testValidSpaced(ValidCase valid) {
		final StringBuilder spacedBuilder = new StringBuilder();
		for (int i = 0; i < valid.predicate.length(); i++) {
			final char c = valid.predicate.charAt(i);
			switch (c) {
				case AND, OR, NOT, OPEN, CLOSE -> spacedBuilder.append(' ').append(c).append("  ");
				default -> spacedBuilder.append(c);
			}
		}

		final Predicate<String> predicate = CONTAINS_PARSER.parse(spacedBuilder.toString());
		testValidImpl(valid, predicate);
	}

	private static void testValidImpl(ValidCase valid, Predicate<String> predicate) {
		for (final String expected : valid.expected) {
			assertTrue(
					predicate.test(expected),
					() -> "Expected predicate '%s' to match '%s'!".formatted(valid.predicate, expected)
			);
		}

		for (final String unexpected : valid.unexpected) {
			assertFalse(
					predicate.test(unexpected),
					() -> "Did not expect predicate '%s' to match '%s'!".formatted(valid.predicate, unexpected)
			);
		}
	}

	private static Stream<ValidCase> streamValidCases() {
		return Stream
				.of(
					validBuilder("a").expect("a", "art", "pita", "zap", "data").unexpect("", "b", "xyz"),
					validBuilder("ab").expect("ab", "lab").unexpect("a", "b", "ba", "xyz"),
					validBuilder("a&b").expect("ab", "ba", "lab", "beard").unexpect("xyz", "as", "orbit"),
					validBuilder("ab&ef").expect("abef", "efab").unexpect("ab", "ef", "abfe", "bafe"),
					validBuilder("e|f").expect("e", "f", "ef", "fe", "leaf", "err", "fcc").unexpect("a", "it"),
					validBuilder("ab|ef").expect("ab", "ef").unexpect("aebf", "beaf"),
					validBuilder("!a").expect("", "b", "we").unexpect("a", "lab"),
					validBuilder("!ab").expect("", "a", "b", "ba", "we").unexpect("ab", "abba"),
					validBuilder("(a&b)|(e&f)").expect("lab", "effect").unexpect("xyz", "ae", "af", "be", "bf"),
					validBuilder("(ab|ef)&(or|ut)")
						.expect("abor", "abut", "efor", "efut")
						.unexpect("ab", "ef", "or", "ut", "abef", "orut"),
					validBuilder("a&!b").expect("a", "ha").unexpect("ab", "ba", "b", "z"),
					validBuilder("!a|b").expect("", "b", "ab", "z").unexpect("a"),
					validBuilder("!(a|b)").expect("", "z").unexpect("a", "b"),
					validBuilder("a&!(b|c)").expect("a", "at").unexpect("z", "ab", "ca"),
					validBuilder("(a|b)&!(c&d)").expect("a", "b", "ac", "bd").unexpect("acd", "dcb"),
					validBuilder("((a|b)&c)&!(d|e)").expect("ac", "bc").unexpect("ace", "cbe")
				)
				.map(ValidCase.Builder::build);
	}

	private static ValidCase.Builder validBuilder(String predicate) {
		return new ValidCase.Builder(predicate);
	}

	@ParameterizedTest
	@MethodSource("streamInvalidCases")
	void testInvalid(String predicate) {
		assertThrows(IllegalArgumentException.class, () -> CONTAINS_PARSER.parse(predicate));
	}

	private static Stream<String> streamInvalidCases() {
		final Supplier<Stream<String>> nonElements = () -> Stream.of(AND, OR, NOT, OPEN, CLOSE).map(Object::toString);

		return Streams.concat(
			nonElements.get(),
			nonElements.get().flatMap(left -> nonElements.get().map(right -> left + right)),
			Stream.of(
				"", "&a", "a&", "|a", "a|", "!!a", "a!!", "!a!",
				"(a", "a(", ")a", "a)", "((a)", "(a))"
			)
		);
	}

	record ValidCase(String predicate, Set<String> expected, Set<String> unexpected) {
		static class Builder {
			final String predicate;
			@Nullable
			Set<String> expect;
			@Nullable
			Set<String> unexpect;

			Builder(String predicate) {
				if (predicate == null || predicate.isEmpty()) {
					throw new IllegalArgumentException("Predicate must not be empty!");
				}

				this.predicate = predicate;
			}

			@SuppressWarnings({"unused", "RedundantThrows"})
			Builder expect() throws Throwable {
				throw new UnsupportedOperationException();
			}

			Builder expect(String... expect) {
				if (this.expect != null) {
					throw new IllegalStateException("Trying to reset expect of '%s'!");
				} else if (expect.length == 0) {
					throw new IllegalStateException("Trying to set empty expect for '%s'!");
				}

				this.expect = Set.of(expect);
				return this;
			}

			@SuppressWarnings({"unused", "RedundantThrows"})
			Builder unexpect() throws Throwable {
				throw new UnsupportedOperationException();
			}

			Builder unexpect(String... unexpect) {
				if (this.unexpect != null) {
					throw new IllegalStateException("Trying to reset unexpect of '%s'!");
				} else if (unexpect.length == 0) {
					throw new IllegalStateException("Trying to set empty unexpect for '%s'!");
				}

				this.unexpect = Set.of(unexpect);
				return this;
			}

			ValidCase build() {
				if (this.expect == null) {
					throw new IllegalStateException("No expect for '%s'!".formatted(this.predicate));
				}

				if (this.unexpect == null) {
					throw new IllegalStateException("No unexpect for '%s'!".formatted(this.predicate));
				}

				return new ValidCase(this.predicate, this.expect, this.unexpect);
			}
		}
	}
}

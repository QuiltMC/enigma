package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.Stack;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Encapsulates logic for parsing a boolean expression of elements and combining those elements into a predicate.
 *
 * <p> Supports {@value #AND}, {@value #OR}, {@value #NOT} and parentheses operators.
 *
 * @param elementParser a function that parses the string representation of an element;
 *                      <em>note</em> that elements' string representations may not contain
 *                      {@value #AND}, {@value #OR}, {@value #NOT}, {@value #OPEN}, {@value #CLOSE}, or space.
 * @param elementPredicator a function used to convert an element into a predicate
 * @param <E> the type of elements
 * @param <O> the tested type
 */
record PredicateParser<E, O>(Function<String, E> elementParser, BiPredicate<E, O> elementPredicator) {
	@VisibleForTesting
	static final char AND = '&';
	@VisibleForTesting
	static final char OR = '|';
	@VisibleForTesting
	static final char NOT = '!';

	@VisibleForTesting
	static final char OPEN = '(';
	@VisibleForTesting
	static final char CLOSE = ')';

	Predicate<O> parse(String string) {
		// non-null only when in the middle of an element
		@Nullable
		StringBuilder elementBuilder = null;
		// null only before any elements have been parsed
		@Nullable
		Predicate<O> currentPredicate = null;
		Token priorToken = Token.NONE;

		// contains nulls for leading or consecutive open parentheses
		final Stack<IncompletePredicate<O>> incompletes = new Stack<>();

		for (int i = 0; i < string.length(); i++) {
			final char c = string.charAt(i);
			switch (c) {
				case ' ' -> {
					if (elementBuilder != null) {
						currentPredicate = this.combine(currentPredicate, elementBuilder.toString(), priorToken);
						elementBuilder = null;
						priorToken = Token.ELEMENT;
					}
				}
				case AND -> {
					if (elementBuilder != null) {
						currentPredicate = this.combine(currentPredicate, elementBuilder.toString(), priorToken);
						elementBuilder = null;
						priorToken = Token.ELEMENT;
					}

					priorToken = switch (priorToken) {
						case AND -> throw consecutiveTokensErrorOf(Token.AND, Token.AND, i);
						case OR -> throw consecutiveTokensErrorOf(Token.OR, Token.AND, i);
						case NOT, AND_NOT, OR_NOT -> throw consecutiveTokensErrorOf(Token.NOT, Token.AND, i);
						case OPEN -> throw consecutiveTokensErrorOf(Token.OPEN, Token.AND, i);
						case CLOSE, ELEMENT -> Token.AND;
						case NONE -> throw leadingTokenErrorOf(Token.AND);
					};
				}
				case OR -> {
					if (elementBuilder != null) {
						currentPredicate = this.combine(currentPredicate, elementBuilder.toString(), priorToken);
						elementBuilder = null;
						priorToken = Token.ELEMENT;
					}

					priorToken = switch (priorToken) {
						case AND -> throw consecutiveTokensErrorOf(Token.AND, Token.OR, i);
						case OR -> throw consecutiveTokensErrorOf(Token.OR, Token.OR, i);
						case NOT, AND_NOT, OR_NOT -> throw consecutiveTokensErrorOf(Token.NOT, Token.OR, i);
						case OPEN -> throw consecutiveTokensErrorOf(Token.OPEN, Token.OR, i);
						case CLOSE, ELEMENT -> Token.OR;
						case NONE -> throw leadingTokenErrorOf(Token.OR);
					};
				}
				case NOT -> {
					if (elementBuilder != null) {
						throw consecutiveTokensErrorOf(Token.ELEMENT, Token.NOT, i);
					}

					priorToken = switch (priorToken) {
						case OPEN, NONE -> Token.NOT;
						case AND -> Token.AND_NOT;
						case OR -> Token.OR_NOT;
						case NOT, AND_NOT, OR_NOT -> throw consecutiveTokensErrorOf(Token.NOT, Token.NOT, i);
						case CLOSE -> throw consecutiveTokensErrorOf(Token.CLOSE, Token.NOT, i);
						case ELEMENT -> throw consecutiveTokensErrorOf(Token.ELEMENT, Token.NOT, i);
					};
				}
				case OPEN -> {
					if (elementBuilder != null) {
						throw consecutiveTokensErrorOf(Token.ELEMENT, Token.OPEN, i);
					}

					incompletes.push(
							switch (priorToken) {
								case AND -> new IncompletePredicate<>(currentPredicate, Operator.AND);
								case OR -> new IncompletePredicate<>(currentPredicate, Operator.OR);
								case NOT -> new IncompletePredicate<>(currentPredicate, Operator.NOT);
								case AND_NOT -> new IncompletePredicate<>(currentPredicate, Operator.AND_NOT);
								case OR_NOT -> new IncompletePredicate<>(currentPredicate, Operator.OR_NOT);
								case OPEN, NONE -> null;
								case CLOSE -> throw consecutiveTokensErrorOf(Token.CLOSE, Token.OPEN, i);
								case ELEMENT -> throw consecutiveTokensErrorOf(Token.ELEMENT, Token.OPEN, i);
							}
					);

					currentPredicate = null;
					priorToken = Token.OPEN;
				}
				case CLOSE -> {
					if (elementBuilder != null) {
						currentPredicate = this.combine(currentPredicate, elementBuilder.toString(), priorToken);
						elementBuilder = null;
						priorToken = Token.ELEMENT;
					}

					currentPredicate = switch (priorToken) {
						case AND -> throw consecutiveTokensErrorOf(Token.AND, Token.CLOSE, i);
						case OR -> throw consecutiveTokensErrorOf(Token.OR, Token.CLOSE, i);
						case NOT, OR_NOT, AND_NOT -> throw consecutiveTokensErrorOf(Token.NOT, Token.CLOSE, i);
						case OPEN -> throw consecutiveTokensErrorOf(Token.OPEN, Token.CLOSE, i);
						case CLOSE, ELEMENT -> {
							if (incompletes.isEmpty()) {
								throw unopenedCloseErrorOf(i);
							}

							final IncompletePredicate<O> incomplete = incompletes.pop();
							if (incomplete != null) {
								yield incomplete.combine(currentPredicate);
							} else {
								yield currentPredicate;
							}
						}
						case NONE -> throw unopenedCloseErrorOf(i);
					};

					priorToken = Token.CLOSE;
				}
				default -> {
					// element part
					if (elementBuilder == null) {
						// start of new element
						if (priorToken == Token.ELEMENT) {
							throw consecutiveTokensErrorOf(Token.ELEMENT, Token.ELEMENT, i);
						}

						elementBuilder = new StringBuilder();
					}

					elementBuilder.append(c);
				}
			}
		}

		if (!incompletes.isEmpty()) {
			throw new IllegalArgumentException("Unclosed parentheses.");
		}

		if (elementBuilder != null) {
			currentPredicate = this.combine(currentPredicate, elementBuilder.toString(), priorToken);
		} else {
			switch (priorToken) {
				case OPEN -> throw trailingTokenErrorOf(Token.OPEN);
				case AND -> throw trailingTokenErrorOf(Token.AND);
				case OR -> throw trailingTokenErrorOf(Token.OR);
				case NOT, AND_NOT, OR_NOT -> throw trailingTokenErrorOf(Token.NOT);
			}
		}

		if (currentPredicate == null) {
			throw new IllegalArgumentException("No elements specified.");
		}

		return currentPredicate;
	}

	private static IllegalArgumentException unopenedCloseErrorOf(int index) {
		return new IllegalArgumentException(
				"Unopened " + Token.CLOSE + " at index %d.".formatted(index)
		);
	}

	private Predicate<O> combine(@Nullable Predicate<O> predicate, String element, Token operator) {
		final E e = this.elementParser.apply(element);
		final Predicate<O> elementPredicate = o -> this.elementPredicator.test(e, o);

		return combine(predicate, elementPredicate, operator);
	}

	private static <O> Predicate<O> combine(@Nullable Predicate<O> left, Predicate<O> right, Token operator) {
		if (left == null) {
			return operator == Token.NOT ? right.negate() : right;
		} else {
			return switch (operator) {
				case AND -> left.and(right);
				case OR -> left.or(right);
				case NOT -> throw new IllegalStateException(
						"Combining predicates separated by non-binary operator!"
				);
				case AND_NOT -> left.and(right.negate());
				case OR_NOT -> left.or(right.negate());
				case OPEN, CLOSE, ELEMENT, NONE -> throw new IllegalStateException(
						"Combining predicates separated by non-operator tokens!"
				);
			};
		}
	}

	private static IllegalArgumentException leadingTokenErrorOf(Token token) {
		return new IllegalArgumentException("Unexpected leading %s.".formatted(token));
	}

	private static IllegalArgumentException trailingTokenErrorOf(Token token) {
		return new IllegalArgumentException("Unexpected trailing %s.".formatted(token));
	}

	private static IllegalArgumentException consecutiveTokensErrorOf(Token first, Token second, int index) {
		return new IllegalArgumentException("Unexpected %s following %s at index %d.".formatted(second, first, index));
	}

	private enum Token {
		AND("'" + PredicateParser.AND + "'"),
		OR("'" + PredicateParser.OR + "'"),
		NOT("'" + PredicateParser.NOT + "'"),
		AND_NOT(AND.description + ", " + NOT.description),
		OR_NOT(OR.description + ", " + NOT.description),
		OPEN("'" + PredicateParser.OPEN + "'"),
		CLOSE("'" + PredicateParser.CLOSE + "'"),
		ELEMENT("element"),
		NONE("no token");

		final String description;

		Token(String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

	private enum Operator {
		AND(Token.AND), OR(Token.OR), NOT(Token.NOT), AND_NOT(Token.AND_NOT), OR_NOT(Token.OR_NOT);

		final Token token;

		Operator(Token token) {
			this.token = token;
		}

		@Override
		public String toString() {
			return this.token.toString();
		}
	}

	private record IncompletePredicate<O>(@Nullable Predicate<O> predicate, Operator operator) {
		Predicate<O> combine(Predicate<O> complete) {
			return PredicateParser.combine(this.predicate, complete, this.operator.token);
		}
	}
}

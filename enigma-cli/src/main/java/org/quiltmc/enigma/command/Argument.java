package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Represents an argument for a {@link Command}.
 *
 * <p> Contains the argument's name and other details, along with the logic to parse it from a string.<br>
 * If parsing fails in a predictable way, an {@link IllegalArgumentException} should be thrown.
 * Note that whether an argument is required is decided per-command; argument parsing should <em>not</em> throw
 * exceptions when the argument is missing. Instead it should return {@code null}.
 *
 * @param <T> the type of the argument
 */
final class Argument<T> {
	@VisibleForTesting
	static final char SEPARATOR = ' ';
	@VisibleForTesting
	static final char NAME_DELIM = '=';

	static final String ALTERNATIVES_DELIM = "|";

	static final String BOOL_TYPE = true + ALTERNATIVES_DELIM + false;
	static final String PATH_TYPE = "path";
	static final String INT_TYPE = "int";
	static final String PATTERN_TYPE = "regex";

	static Argument<Path> ofPath(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, string -> parsePath(string).orElse(null), explanation);
	}

	static Argument<Path> ofFile(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseFile, explanation);
	}

	static Argument<Path> ofFolder(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseFolder, explanation);
	}

	static Argument<Path> ofReadablePath(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseReadablePath, explanation);
	}

	static Argument<Path> ofReadableFile(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseReadableFile, explanation);
	}

	static Argument<Path> ofReadableFolder(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseReadableFolder, explanation);
	}

	static Argument<Path> ofWritablePath(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseWritablePath, explanation);
	}

	static Argument<Path> ofWritableFile(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseWritableFile, explanation);
	}

	static Argument<Path> ofWritableFolder(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::parseWritableFolder, explanation);
	}

	/**
	 * Creates a string argument whose {@code typeDescription} lists expected values.
	 */
	static Argument<String> ofLenientEnum(String name, Class<? extends Enum<?>> type, String explanation) {
		return ofString(name, alternativesOf(type), explanation);
	}

	/**
	 * Creates an enum argument that is case-insensitive.<br>
	 * Assumes enum values have conventional {@code SCREAMING_SNAKE_CASE} names.
	 */
	static <E extends Enum<E>> Argument<E> ofEnum(String name, Class<E> type, String explanation) {
		return new Argument<>(name, alternativesOf(type), string -> parseCaseInsensitiveEnum(type, string), explanation);
	}

	static <E extends Enum<E>> Argument<E> ofStrictEnum(String name, Class<E> type, String explanation) {
		return new Argument<>(name, alternativesOf(type), string -> Enum.valueOf(type, string), explanation);
	}

	static Argument<Boolean> ofBool(String name, String explanation) {
		return new Argument<>(name, BOOL_TYPE, Boolean::parseBoolean, explanation);
	}

	static Argument<Integer> ofInt(String name, String explanation) {
		return new Argument<>(name, INT_TYPE, Argument::parseInt, explanation);
	}

	static Argument<String> ofString(String name, String typeDescription, String explanation) {
		return new Argument<>(name, typeDescription, Argument::parseString, explanation);
	}

	static Argument<Pattern> ofPattern(String name, String explanation) {
		return new Argument<>(name, PATTERN_TYPE, Argument::parsePattern, explanation);
	}

	static <T, C extends Collection<T>> Argument<C> ofCollection(
			String name, String typeDescription, String explanation,
			Function<String, T> elementParser, Collector<T, ?, C> collector
	) {
		return new Argument<>(
				name, typeDescription,
				string -> parseCollection(string, ",", elementParser, collector),
				explanation
		);
	}

	private final String name;
	private final Function<String, T> fromString;
	private final String displayForm;
	private final String explanation;

	/**
	 * Creates an argument.
	 *
	 * <p> See static factory methods for common argument types.
	 *
	 * @param name the name of the argument; may not contain any space or {@value #NAME_DELIM} characters
	 * @param typeDescription a short description of the type of value to expect; conventional descriptions are in
	 *                          kebab-case with alternatives separated by {@value ALTERNATIVES_DELIM}
	 * @param explanation an extended explanation of what the argument accepts and what it's for
	 */
	Argument(String name, String typeDescription, Function<String, T> fromString, String explanation) {
		this.name = name;
		this.displayForm = "[" + this.name + NAME_DELIM + "]" + "<" + typeDescription + ">";
		this.fromString = fromString;
		this.explanation = explanation;
	}

	static Path parseFile(String path) {
		return verifyFile(parsePath(path)).orElse(null);
	}

	static <T, C extends Collection<T>> C parseCollection(
			String input, String delimRegex, Function<String, T> elementParser, Collector<T, ?, C> collector
	) {
		return Arrays.stream(input.split(delimRegex))
				.map(string -> {
					final T element = elementParser.apply(string);
					if (element == null) {
						throw new IllegalArgumentException("Invalid element: " + string);
					} else {
						return element;
					}
				})
				.collect(collector);
	}

	static Path parseFolder(String path) {
		return verifyFolder(parsePath(path)).orElse(null);
	}

	static Path parseReadablePath(String path) {
		return parseExistentPath(path).orElse(null);
	}

	static Path parseReadableFile(String path) {
		return verify(parseExistentPath(path), Files::isRegularFile, "Not a file: ").orElse(null);
	}

	static Path parseReadableFolder(String path) {
		return parseExistentFolder(path).orElse(null);
	}

	static Path parseWritablePath(String path) {
		return parseParentedPath(path).orElse(null);
	}

	static Path parseWritableFile(String path) {
		return verifyFile(parseParentedPath(path)).orElse(null);
	}

	static Path parseWritableFolder(String path) {
		return parseExistentFolder(path).orElse(null);
	}

	static Optional<Path> parseExistentFolder(String path) {
		return verify(parseExistentPath(path), Files::isDirectory, "Not a folder: ");
	}

	static Optional<Path> parseExistentPath(String path) {
		return verify(parsePath(path), Files::exists, "Cannot find path: ");
	}

	static Optional<Path> parseParentedPath(String path) {
		return peek(parsePath(path), child -> {
			final Path parent = child.getParent();
			if (parent == null) {
				throw new IllegalArgumentException("Cannot write path: " + child);
			}

			try {
				Files.createDirectories(parent);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	static Optional<Path> parsePath(String path) {
		return Optional.of(path)
			.filter(string -> !string.isEmpty())
			.map(Paths::get)
			.map(Path::toAbsolutePath);
	}

	static String parseString(String string) {
		return string.isEmpty() ? null : string;
	}

	static <E extends Enum<E>> E parseCaseInsensitiveEnum(Class<E> type, String string) {
		return Enum.valueOf(type, string.toUpperCase());
	}

	static Pattern parsePattern(String regex) {
		if (regex.isEmpty()) {
			return null;
		}

		try {
			return Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	static Integer parseInt(String integer) {
		if (integer.isEmpty()) {
			return null;
		} else {
			try {
				return Integer.parseInt(integer);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	static Optional<Path> verifyFile(Optional<Path> path) {
		// !directory so it's true for non-existent files
		return verify(path, p -> !Files.isDirectory(p), "Not a file: ");
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	static Optional<Path> verifyFolder(Optional<Path> path) {
		// !directory so it's true for non-existent folders
		return verify(path, p -> !Files.isRegularFile(p), "Not a file: ");
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	static <T> Optional<T> verify(Optional<T> optional, Predicate<T> verify, String messagePrefix) {
		return peek(optional, value -> {
			if (!verify.test(value)) {
				throw new IllegalArgumentException(messagePrefix + value);
			}
		});
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	static <T> Optional<T> peek(Optional<T> optional, Consumer<T> action) {
		optional.ifPresent(action);
		return optional;
	}

	static String alternativesOf(Class<? extends Enum<?>> type) {
		return alternativesOf(type, ALTERNATIVES_DELIM);
	}

	static String alternativesOf(Class<? extends Enum<?>> type, String delim) {
		return Arrays.stream(type.getEnumConstants())
			.map(Object::toString)
			.collect(Collectors.joining(delim));
	}

	public String getName() {
		return this.name;
	}

	public String getExplanation() {
		return this.explanation;
	}

	public String getDisplayForm() {
		return this.displayForm;
	}

	@Nullable
	T from(Map<String, String> args) {
		final String string = args.get(this.name);
		return string == null ? null : this.fromString.apply(string);
	}

	T requireFrom(Map<String, String> values) {
		final T t = this.from(values);
		if (t == null) {
			throw new IllegalArgumentException(this.name + " is required");
		}

		return t;
	}
}

package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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

	static Argument<Path> ofReadablePath(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getReadablePath, explanation);
	}

	static Argument<Path> ofReadableFile(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getReadableFile, explanation);
	}

	static Argument<Path> ofReadableFolder(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getReadableFolder, explanation);
	}

	static Argument<Path> ofWritablePath(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getWritablePath, explanation);
	}

	static Argument<Path> ofWritableFile(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getWritableFile, explanation);
	}

	static Argument<Path> ofWritableFolder(String name, String explanation) {
		return new Argument<>(name, PATH_TYPE, Argument::getWritableFolder, explanation);
	}

	/**
	 * Creates a string argument whose {@code typeDescription} lists expected values.
	 */
	static Argument<String> ofLenientEnum(String name, Class<? extends Enum<?>> type, String explanation) {
		final String alternatives = Arrays.stream(type.getEnumConstants())
				.map(Object::toString)
				.collect(Collectors.joining(ALTERNATIVES_DELIM));
		return ofString(name, alternatives, explanation);
	}

	static Argument<Boolean> ofBool(String name, String explanation) {
		return new Argument<>(name, BOOL_TYPE, Boolean::parseBoolean, explanation);
	}

	static Argument<String> ofString(String name, String typeDescription, String explanation) {
		return new Argument<>(name, typeDescription, Function.identity(), explanation);
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

	static Path getReadablePath(String path) {
		return getExistentPath(path).orElse(null);
	}

	static Path getReadableFile(String path) {
		return verify(getExistentPath(path), Files::isRegularFile, "Not a file: ").orElse(null);
	}

	static Path getReadableFolder(String path) {
		return getExistentFolder(path).orElse(null);
	}

	static Path getWritablePath(String path) {
		return getParentedPath(path).orElse(null);
	}

	static Path getWritableFile(String path) {
		// !directory so it's true for non-existent files
		return verify(getParentedPath(path), p -> !Files.isDirectory(p), "Not a file: ").orElse(null);
	}

	static Path getWritableFolder(String path) {
		return getExistentFolder(path).orElse(null);
	}

	static Optional<Path> getExistentFolder(String path) {
		return verify(getExistentPath(path), Files::isDirectory, "Not a folder: ");
	}

	static Optional<Path> getExistentPath(String path) {
		return verify(getPath(path), Files::exists, "Cannot find path: ");
	}

	static Optional<Path> getParentedPath(String path) {
		return peek(getPath(path), child -> {
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

	static Optional<Path> getPath(String path) {
		return Optional.ofNullable(path)
			.filter(string -> !string.isEmpty())
			.map(Paths::get)
			.map(Path::toAbsolutePath);
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
		return this.fromString.apply(args.get(this.name));
	}

	T requireFrom(Map<String, String> values) {
		final T t = this.from(values);
		if (t == null) {
			throw new IllegalArgumentException(this.name + " is required");
		}

		return t;
	}
}

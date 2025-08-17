package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.util.Either;
import org.quiltmc.enigma.command.GrepMappingsCommand.Required;
import org.quiltmc.enigma.command.GrepMappingsCommand.Optionals;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class GrepMappingsCommand extends Command<Required, Optionals> {
	private static final String VOID = "void";

	private static final Argument<Pattern> CLASSES = Argument.ofPattern("classes",
			"""
			A regular expression to filter class names."""
	);
	private static final Argument<Pattern> METHODS = Argument.ofPattern("methods",
			"""
			A regular expression to filter method names."""
	);
	private static final Argument<Pattern> FIELDS = Argument.ofPattern("fields",
			"""
			A regular expression to filter field names."""
	);
	private static final Argument<Pattern> PARAMS = Argument.ofPattern("params",
			"""
			A regular expression to filter parameter names."""
	);
	private static final Argument<Either<Class<Void>, Pattern>> METHOD_RETURNS = new Argument<>(
			"method-returns", VOID + "void|pattern",
			string -> VOID.equals(string) ? Either.left(Void.class) : Either.right(Argument.parsePattern(string)),
			"""
			A regular expression to filter method return type names. Pass 'void' to filter void methods."""
	);
	private static final Argument<Pattern> FIELD_TYPES = Argument.ofPattern("field-types",
			"""
			A regular expression to filter field type names."""
	);
	private static final Argument<Pattern> PARAM_TYPES = Argument.ofPattern("param-types",
			"""
			A regular expression to filter parameter type names."""
	);

	public static final GrepMappingsCommand INSTANCE = new GrepMappingsCommand();

	private GrepMappingsCommand() {
		super(
				ArgsParser.of(CommonArguments.INPUT_JAR, CommonArguments.INPUT_MAPPINGS, Required::new),
				ArgsParser.of(CLASSES, METHODS, FIELDS, PARAMS, METHOD_RETURNS, FIELD_TYPES, PARAM_TYPES, Optionals::new)
		);
	}

	@Override
	void runImpl(Required required, Optionals optionals) throws Exception {
		final Path jar = required.inputJar;
		final Path mappings = required.inputMappings;

		final Pattern classes = optionals.classes;
		final Pattern methods = optionals.methods;
		final Pattern fields = optionals.fields;
		final Pattern params = optionals.params;

		final Either<Class<Void>, Pattern> methodReturns = optionals.methodReturns;

		final Pattern fieldTypes = optionals.fieldTypes;
		final Pattern paramTypes = optionals.paramTypes;



		run(jar, mappings, classes, methods, fields, params, methodReturns, fieldTypes, paramTypes);
	}

	@Override
	public String getName() {
		return "grep-mappings";
	}

	@Override
	public String getDescription() {
		return "Searches for class and/or member names using regular expression. "
				+ "Members can additionally be filtered by type.";
	}

	public static void runVoidMethodReturns(
		Path jar, Path mappings,
		@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern params,
		@Nullable Pattern fieldTypes, @Nullable Pattern paramTypes
	) throws Exception {
		run(
			jar, mappings, classes, methods, fields, params,
			Either.left(Void.class),
			fieldTypes, paramTypes
		);
	}

	public static void run(
		Path jar, Path mappings,
		@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern params,
		@Nullable Pattern methodsReturns,
		@Nullable Pattern fieldTypes, @Nullable Pattern paramTypes
	) throws Exception {
		run(
				jar, mappings, classes, methods, fields, params,
				methodsReturns == null ? null : Either.right(methodsReturns),
				fieldTypes, paramTypes
		);
	}

	private static void run(
			Path jar, Path mappings,
			@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern params,
			@Nullable Either<Class<Void>, Pattern> methodsReturns,
			@Nullable Pattern fieldTypes, @Nullable Pattern paramTypes
	) throws Exception {
		Objects.requireNonNull(jar, "jar must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		final EnigmaProject project = openProject(jar, mappings);

		Logger.info("Grepping mappings...");


	}

	private record Filter(Predicate<String> namePredicate, ResultType type) {
		private static Filter of(Pattern pattern, ResultType type) {
			return new Filter(name -> pattern.matcher(name).find(), type);
		}

		private static Filter ofVoidMethods() {

		}

		Optional<Map.Entry<ResultType, String>> findResult(String name, String obfName) {
			return this.namePredicate.test(name) ?
				java.util.Optional.of(Map.entry(this.type, "%s (%s)".formatted(name, obfName)))
				: java.util.Optional.empty();
		}
	}

	private enum ResultType {
		CLASS("class", "classes"),
		METHOD("method", "methods"),
		FIELD("field", "fields"),
		PARAM("param", "params");

		final String singleName;
		final String pluralName;

		ResultType(String singleName, String pluralName) {
			this.singleName = singleName;
			this.pluralName = pluralName;
		}

		String getResultsHeader(int resultCount) {
			return "Found %d %s:\n\t".formatted(resultCount, resultCount == 1 ? this.singleName : this.pluralName);
		}
	}

	record Required(Path inputJar, Path inputMappings) { }
	record Optionals(
			Pattern classes, Pattern methods, Pattern fields, Pattern params,
			Either<Class<Void>, Pattern> methodReturns, Pattern fieldTypes, Pattern paramTypes
	) { }
}

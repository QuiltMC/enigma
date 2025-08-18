package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.command.GrepMappingsCommand.Required;
import org.quiltmc.enigma.command.GrepMappingsCommand.Optionals;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO
 */
public final class GrepMappingsCommand extends Command<Required, Optionals> {
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
	private static final Argument<Pattern> METHOD_RETURNS = Argument.ofPattern("method-returns",
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
	private static final Argument<Integer> LIMIT = Argument.ofInt("limit",
			"""
			A limit on the number of individual results which may be displayed per result type (class, field, method, parameter).
			The total, unlimited result count is always reported.
			A limit of 0 causes only counts to be reported and negative limits are ignored."""
	);

	public static final GrepMappingsCommand INSTANCE = new GrepMappingsCommand();

	private GrepMappingsCommand() {
		super(
				ArgsParser.of(CommonArguments.INPUT_JAR, CommonArguments.INPUT_MAPPINGS, Required::new),
				ArgsParser.of(
					CLASSES, METHODS, METHOD_RETURNS, FIELDS, FIELD_TYPES, PARAMS, PARAM_TYPES, LIMIT,
					Optionals::new
				)
		);
	}

	@Override
	void runImpl(Required required, Optionals optionals) throws Exception {
		run(
				required.inputJar, required.inputMappings,
				optionals.classes, optionals.methods, optionals.methodReturns, optionals.fields, optionals.fieldTypes, optionals.params,
				optionals.paramTypes,
				optionals.limit == null ? -1 : optionals.limit
		);
	}

	@Override
	public String getName() {
		return "grep-mappings";
	}

	@Override
	public String getDescription() {
		return "Searches for class and/or member names using regular expressions. "
				+ "Members can additionally be filtered by type.";
	}

	public static void run(
			Path jar, Path mappings,
			@Nullable Pattern classes,
			@Nullable Pattern methods, @Nullable Pattern methodsReturns,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes,
			@Nullable Pattern parameters, @Nullable Pattern parameterTypes,
			int limit
	) throws Exception {
		final String message = runImpl(
				jar, mappings,
				classes, methods, methodsReturns, fields, fieldTypes, parameters,
				parameterTypes, limit
		);

		if (message.isEmpty()) {
			Logger.warn("No matches");
		} else {
			Logger.info(message);
		}
	}

	@VisibleForTesting
	static String runImpl(
			Path jar, Path mappings,
			@Nullable Pattern classes,
			@Nullable Pattern methods, @Nullable Pattern methodsReturns,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes,
			@Nullable Pattern parameters, @Nullable Pattern parameterTypes,
			int limit
	) throws Exception {
		Objects.requireNonNull(jar, "jar must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		final EnigmaProject project = openProject(jar, mappings);
		final EntryIndex entryIndex = project.getJarIndex().getIndex(EntryIndex.class);
		final Translator deobfuscator = project.getRemapper().getDeobfuscator();

		final Optional<Finder<ClassEntry>> classFinder = Finder.ofClassesOrEmpty(classes, deobfuscator);
		final Optional<Finder<MethodEntry>> methodFinder = Finder.ofOrEmpty(
				methods, methodsReturns, deobfuscator,
				ResultType.METHOD, method -> method.getDesc().getReturnDesc()
		);
		final Optional<Finder<FieldEntry>> fieldFinder = Finder.ofOrEmpty(
				fields, fieldTypes, deobfuscator,
				ResultType.FIELD, FieldEntry::getDesc
		);
		final Optional<Finder<LocalVariableDefEntry>> paramFinder = Finder.ofOrEmpty(
				parameters, parameterTypes, deobfuscator,
				ResultType.PARAM, LocalVariableDefEntry::getDesc
		);

		Logger.info("Grepping mappings...");

		final Multimap<ResultType, String> resultsByType = project.getRemapper().getMappings().getAllEntries()
				.parallel()
				.<Map.Entry<ResultType, String>>mapMulti((obf, add) -> {
					if (obf instanceof ClassEntry obfClass) {
						classFinder.flatMap(finder -> finder.apply(obfClass)).ifPresent(add);
					} else if (obf instanceof MethodEntry obfMethod) {
						methodFinder.flatMap(finder -> finder.apply(obfMethod)).ifPresent(add);

						paramFinder.ifPresent(finder -> {
							obfMethod.streamParameters(entryIndex).forEach(obfParam -> {
								finder.apply(obfParam).ifPresent(add);
							});
						});
					} else if (obf instanceof FieldEntry obfField) {
						fieldFinder.flatMap(finder -> finder.apply(obfField)).ifPresent(add);
					}
				})
				.collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));

		if (resultsByType.isEmpty()) {
			return "";
		} else {
			return "\n" + resultsByType.asMap().entrySet().stream()
				.map(entry -> {
					final ResultType type = entry.getKey();
					final Collection<String> results = entry.getValue();

					final StringBuilder message = type.buildResultHeader(new StringBuilder(), results.size());

					if (limit == 0) {
						message.append('.');
					} else {
						final String delim = "\n\t";
						message.append(':').append(delim);

						if (limit < 0 || results.size() <= limit) {
							message.append(String.join(delim, results));
						} else {
							message.append(String.join(delim, results.stream().limit(limit).toList()));
							final int excess = results.size() - limit;
							message.append(delim).append("... and ").append(excess).append(" more ")
									.append(type.getNameForCount(excess)).append('.');
						}
					}

					return message.toString();
				})
				.collect(Collectors.joining("\n"));
		}
	}

	private static String getName(TypeDescriptor desc, Translator deobfuscator) {
		if (desc.isVoid()) {
			return "void";
		} else if (desc.isPrimitive()) {
			return desc.getPrimitive().getKeyword();
		} else if (desc.isType()) {
			final ClassEntry obf = desc.getTypeEntry();
			final ClassEntry deobf = deobfuscator.translate(obf);
			return getClassName(deobf == null ? obf : deobf);
		} else if (desc.isArray()) {
			return getName(desc.getArrayType(), deobfuscator) + "[]".repeat(desc.getArrayDimension());
		} else {
			throw new IllegalStateException("TypeDescriptor is not void, primitive, type, or array: " + desc);
		}
	}

	private static String getClassName(ClassEntry entry) {
		final ArrayList<String> builder = new ArrayList<>();

		while (entry.getParent() != null) {
			builder.add(0, entry.getName());
		}

		builder.add(0, entry.getName().replace('/', '.'));

		return String.join(".", builder);
	}

	@FunctionalInterface
	private interface Finder<E extends Entry<?>> extends Function<E, Optional<Map.Entry<ResultType, String>>> {
		static Optional<Finder<ClassEntry>> ofClassesOrEmpty(@Nullable Pattern pattern, Translator deobfuscator) {
			if (pattern == null) {
				return Optional.empty();
			} else {
				return Optional.of(ofTypeless(pattern, deobfuscator, ResultType.CLASS));
			}
		}

		static <E extends Entry<?>> Optional<Finder<E>> ofOrEmpty(
				@Nullable Pattern pattern, @Nullable Pattern typePattern, Translator deobfuscator,
				ResultType resultType, Function<E, TypeDescriptor> descriptorGetter
		) {
			if (pattern == null) {
				if (typePattern == null) {
					return Optional.empty();
				} else {
					return Optional.of(obf -> {
						if (typePattern.matcher(getName(descriptorGetter.apply(obf), deobfuscator)).find()) {
							final E deobf = deobfuscator.translate(obf);
							return Optional.of(createResult(resultType, obf, deobf == null ? obf : deobf));
						} else {
							return Optional.empty();
						}
					});
				}
			} else if (typePattern == null) {
				return Optional.of(ofTypeless(pattern, deobfuscator, resultType));
			} else {
				return Optional.of(obf -> {
					final E deobf = deobfuscator.translate(obf);
					if (deobf != null && pattern.matcher(deobf.getName()).find()) {
						if (typePattern.matcher(getName(descriptorGetter.apply(obf), deobfuscator)).find()) {
							return Optional.of(createResult(resultType, obf, deobf));
						} else {
							return Optional.empty();
						}
					} else {
						return Optional.empty();
					}
				});
			}
		}

		private static <E extends Entry<?>> Finder<E> ofTypeless(
				Pattern pattern, Translator deobfuscator, ResultType resultType
		) {
			return obf -> {
				final E deobf = deobfuscator.translate(obf);
				if (deobf != null && pattern.matcher(deobf.getName()).find()) {
					return Optional.of(createResult(resultType, obf, deobf));
				} else {
					return Optional.empty();
				}
			};
		}

		private static Map.Entry<ResultType, String> createResult(ResultType resultType, Entry<?> obf, Entry<?> deobf) {
			return Map.entry(resultType, "%s (%s)".formatted(deobf.getName(), obf.getFullName()));
		}
	}

	@VisibleForTesting
	enum ResultType {
		CLASS("class", "classes"),
		METHOD("method", "methods"),
		FIELD("field", "fields"),
		PARAM("param", "params");

		@VisibleForTesting
		final String singleName;
		@VisibleForTesting
		final String pluralName;

		ResultType(String singleName, String pluralName) {
			this.singleName = singleName;
			this.pluralName = pluralName;
		}

		private String getNameForCount(int resultCount) {
			return resultCount == 1 ? this.singleName : this.pluralName;
		}

		@VisibleForTesting
		StringBuilder buildResultHeader(StringBuilder message, int count) {
			return message.append("Found ").append(count)
				.append(' ').append(this.getNameForCount(count));
		}
	}

	record Required(Path inputJar, Path inputMappings) { }
	record Optionals(
			Pattern classes,
			Pattern methods, Pattern methodReturns,
			Pattern fields, Pattern fieldTypes,
			Pattern params, Pattern paramTypes,
			Integer limit
	) { }
}

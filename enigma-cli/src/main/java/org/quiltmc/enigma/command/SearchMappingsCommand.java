package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.objectweb.asm.Opcodes;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.command.SearchMappingsCommand.Required;
import org.quiltmc.enigma.command.SearchMappingsCommand.Optionals;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.quiltmc.enigma.util.Utils.andJoin;
import static java.util.Comparator.comparingInt;

public final class SearchMappingsCommand extends Command<Required, Optionals> {
	private static final PredicateParser<Access, AccessFlags> ACCESS_PREDICATE_PARSER = new PredicateParser<>(
			Access::valueOf,
			(access, flags) -> (flags.getFlags() & access.flag) != 0
	);

	private static final Argument<Pattern> CLASSES = Argument.ofPattern("classes",
			"""
			A regular expression to filter class names."""
	);
	private static final Argument<Predicate<AccessFlags>> CLASS_ACCESS = accessPredicateArgOf("class-access",
			"""
			An access expression to filter classes."""
	);
	private static final Argument<Pattern> METHODS = Argument.ofPattern("methods",
			"""
			A regular expression to filter method names."""
	);
	private static final Argument<Pattern> METHOD_RETURNS = Argument.ofPattern("method-returns",
			"""
			A regular expression to filter method return type names. Pass 'void' to filter void methods."""
	);
	private static final Argument<Predicate<AccessFlags>> METHOD_ACCESS = accessPredicateArgOf("method-access",
			"""
			An access expression to filter methods."""
	);
	private static final Argument<Pattern> FIELDS = Argument.ofPattern("fields",
			"""
			A regular expression to filter field names."""
	);
	private static final Argument<Pattern> FIELD_TYPES = Argument.ofPattern("field-types",
			"""
			A regular expression to filter field type names."""
	);
	private static final Argument<Predicate<AccessFlags>> FIELD_ACCESS = accessPredicateArgOf("field-access",
			"""
			An access expression to filter fields."""
	);
	private static final Argument<Pattern> PARAMS = Argument.ofPattern("params",
			"""
			A regular expression to filter parameter names."""
	);
	private static final Argument<Pattern> PARAM_TYPES = Argument.ofPattern("param-types",
			"""
			A regular expression to filter parameter type names."""
	);
	private static final Argument<Predicate<AccessFlags>> PARAM_ACCESS = accessPredicateArgOf("param-access",
			"""
			An access expression to filter parameters."""
	);
	private static final Argument<Sort> SORT = Argument.ofEnum("sort", Sort.class,
			"""
			How results should be sorted.""");
	private static final Argument<Integer> LIMIT = Argument.ofInt("limit",
			"""
			A limit on the number of individual results to display per result type (class, method, field, parameter).
			The total, unlimited result count is always reported.
			A limit of 0 causes only counts to be reported and negative limits are ignored."""
	);

	public static final SearchMappingsCommand INSTANCE = new SearchMappingsCommand();

	private static final String DESCRIPTION = """
			Searches for class, method, field, and parameter names using regular expressions.
				Each can additionally be filtered using access expressions; methods, fields, and parameters can be filtered by type.
				An access expression is a boolean combination of access flag keywords. They support &, |, !, and parentheses.
				Available access flag keywords are %s.""".formatted(andJoin(Arrays.stream(Access.values()).map(Object::toString).toList()));

	private SearchMappingsCommand() {
		super(
				ArgsParser.of(CommonArguments.INPUT_JAR, CommonArguments.INPUT_MAPPINGS, Required::new),
				ArgsParser.of(
					CLASSES, CLASS_ACCESS,
					METHODS, METHOD_RETURNS, METHOD_ACCESS,
					FIELDS, FIELD_TYPES, FIELD_ACCESS,
					PARAMS, PARAM_TYPES, PARAM_ACCESS,
					SORT, LIMIT,
					Optionals::new
				)
		);
	}

	@Override
	void runImpl(Required required, Optionals optionals) throws Exception {
		run(
				required.inputJar, required.inputMappings,
				optionals.classes, optionals.classAccess,
				optionals.methods, optionals.methodReturns, optionals.methodAccess,
				optionals.fields, optionals.fieldTypes, optionals.fieldAccess,
				optionals.params, optionals.paramTypes, optionals.paramAccess,
				optionals.sort, optionals.limit == null ? -1 : optionals.limit
		);
	}

	@Override
	public String getName() {
		return "search-mappings";
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	public static void run(
			Path jar, Path mappings,
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodsReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramType, @Nullable Predicate<AccessFlags> paramAccess,
			@Nullable Sort sort, int limit
	) throws Exception {
		final String message = runImpl(
				jar, mappings,
				classes, classAccess,
				methods, methodsReturns, methodAccess,
				fields, fieldTypes, fieldAccess,
				params, paramType, paramAccess,
				sort, limit
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
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodsReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramTypes, @Nullable Predicate<AccessFlags> paramAccess,
			@Nullable Sort sort, int limit
	) throws Exception {
		Objects.requireNonNull(jar, "jar must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		final Sort defaultedSort = sort == null ? Sort.DEFAULT : sort;

		final EnigmaProject project = openProject(jar, mappings);
		final EntryIndex entryIndex = project.getJarIndex().getIndex(EntryIndex.class);
		final EntryRemapper remapper = project.getRemapper();

		final Optional<Finder<ClassEntry>> classFinder =
				Finder.ofClassesOrEmpty(classes, classAccess, remapper, entryIndex);
		final Optional<Finder<MethodEntry>> methodFinder = Finder.ofOrEmpty(
				methods, methodsReturns, methodAccess, ResultType.METHOD,
				remapper, entryIndex, method -> method.getDesc().getReturnDesc()
		);
		final Optional<Finder<FieldEntry>> fieldFinder = Finder.ofOrEmpty(
				fields, fieldTypes, fieldAccess, ResultType.FIELD, remapper, entryIndex, FieldEntry::getDesc
		);
		final Optional<Finder<LocalVariableDefEntry>> paramFinder = Finder.ofOrEmpty(
				params, paramTypes, paramAccess, ResultType.PARAM, remapper, entryIndex, LocalVariableDefEntry::getDesc
		);

		Logger.info("Searching mappings...");

		final Multimap<ResultType, Entry<?>> resultsByType = remapper.getMappings().getAllEntries()
				.parallel()
				.<Map.Entry<ResultType, Entry<?>>>mapMulti((obf, add) -> {
					if (obf instanceof ClassEntry obfClass) {
						classFinder.flatMap(finder -> finder.apply(obfClass)).ifPresent(add);
					} else if (obf instanceof MethodEntry obfMethod) {
						methodFinder.flatMap(finder -> finder.apply(obfMethod)).ifPresent(add);
					} else if (obf instanceof FieldEntry obfField) {
						fieldFinder.flatMap(finder -> finder.apply(obfField)).ifPresent(add);
					} else if (obf instanceof LocalVariableEntry obfParam && obfParam.isArgument()) {
						final LocalVariableDefEntry obfParamDef = entryIndex.getDefinition(obfParam);
						if (obfParamDef != null) {
							paramFinder.flatMap(finder -> finder.apply(obfParamDef)).ifPresent(add);
						}
					}
				})
				.collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));

		if (resultsByType.isEmpty()) {
			return "";
		} else {
			return resultsByType.asMap().entrySet().stream()
				.map(resultEntry -> {
					final ResultType type = resultEntry.getKey();
					final Collection<Entry<?>> resultEntries = resultEntry.getValue();
					final int totalResults = resultEntries.size();
					final boolean limited = limit >= 0 && limit < totalResults;

					final Stream<ResultBuilder> sortedBuilders = resultEntries.stream()
							.map(obf -> new ResultBuilder(QualifiedName.of(remapper.deobfuscate(obf)), obf))
							.sorted(ResultBuilder.comparingName(defaultedSort.comparator));

					final List<String> lines = (limited ? sortedBuilders.limit(limit) : sortedBuilders)
							.map(builder -> {
								final StringBuilder lineBuilder = new StringBuilder();
								if (defaultedSort == Sort.NAME) {
									final int finalNameIndex = builder.name.names.size() - 1;

									lineBuilder
											.append(builder.name.names.get(finalNameIndex))
											.append(" ");

									final ImmutableList<String> outerNames = builder.name.names.subList(0, finalNameIndex);
									if (!(outerNames.isEmpty() && builder.name.packages.isEmpty())) {
										lineBuilder
												.append("(")
												.append(Stream
													.concat(
														builder.name.packages.stream(),
														outerNames.stream()
													)
													.collect(Collectors.joining("."))
												)
												.append(") ");
									}
								} else {
									lineBuilder.append(builder.name.toString());
								}

								lineBuilder.append("[").append(builder.obf.getFullName()).append("]");

								return lineBuilder.toString();
							})
							.toList();

					final StringBuilder message = type.buildResultHeader(new StringBuilder(), totalResults);

					if (limit == 0) {
						message.append('.');
					} else {
						final String delim = "\n\t";
						message.append(':').append(delim).append(String.join(delim, lines));

						if (limited) {
							final int excess = lines.size() - limit;
							message.append(delim).append("... and ").append(excess).append(" more ")
									.append(type.getNameForCount(excess)).append('.');
						}
					}

					return message.toString();
				})
				.collect(Collectors.joining("\n", "\n", ""));
		}
	}

	private static String getTypeName(TypeDescriptor desc, EntryRemapper remapper) {
		if (desc.isVoid()) {
			return "void";
		} else if (desc.isPrimitive()) {
			return desc.getPrimitive().getKeyword();
		} else if (desc.isType()) {
			return QualifiedName.of(remapper.deobfuscate(desc.getTypeEntry())).toString();
		} else if (desc.isArray()) {
			return getTypeName(desc.getArrayType(), remapper) + "[]".repeat(desc.getArrayDimension());
		} else {
			throw new IllegalStateException("TypeDescriptor is not void, primitive, type, or array: " + desc);
		}
	}

	private static Argument<Predicate<AccessFlags>> accessPredicateArgOf(String name, String explanation) {
		return new Argument<>(name, "access-expression", ACCESS_PREDICATE_PARSER::parse, explanation);
	}

	private record ResultBuilder(QualifiedName name, Entry<?> obf) {
		static Comparator<ResultBuilder> comparingName(Comparator<QualifiedName> nameComparator) {
			return (left, right) -> nameComparator.compare(left.name, right.name);
		}
	}

	private record QualifiedName(ImmutableList<String> packages, ImmutableList<String> names) {
		static final Comparator<List<String>> PARTS_ALPHABETIZER = (left, right) -> {
			if (left.isEmpty()) {
				return right.isEmpty() ? 0 : -1;
			}

			for (int i = 0; i < left.size(); i++) {
				if (i >= right.size()) {
					return 1;
				} else {
					final int comparison = left.get(i).compareTo(right.get(i));
					if (comparison != 0) {
						return comparison;
					}
				}
			}

			return 0;
		};

		static final Comparator<QualifiedName> NAMES_ALPHABETIZER = (left, right) ->
				PARTS_ALPHABETIZER.compare(left.names, right.names);

		static final Comparator<QualifiedName> PACKAGES_ALPHABETIZER = (left, right) ->
				PARTS_ALPHABETIZER.compare(left.packages, right.packages);

		static final Comparator<QualifiedName> PACKAGES_DEPTH_SORTER =
				comparingInt(qualified -> qualified.packages.size());

		static QualifiedName of(Entry<?> entry) {
			final ArrayList<String> names = new ArrayList<>();

			while (entry.getParent() != null) {
				names.add(0, entry.getName());
				entry = entry.getParent();
			}

			final String[] qualifiedTopName = entry.getFullName().split("/");
			final int lastIndex = qualifiedTopName.length - 1;
			names.add(0, qualifiedTopName[lastIndex]);

			final ImmutableList.Builder<String> packages = ImmutableList.builder();
			for (int i = 0; i < lastIndex; i++) {
				packages.add(qualifiedTopName[i]);
			}

			return new QualifiedName(packages.build(), ImmutableList.copyOf(names));
		}

		@Override
		public String toString() {
			return Stream
					.concat(this.packages.stream(), this.names.stream())
					.collect(Collectors.joining("."));
		}
	}

	@FunctionalInterface
	private interface Finder<E extends Entry<?>> extends Function<E, Optional<Map.Entry<ResultType, Entry<?>>>> {
		static Optional<Finder<ClassEntry>> ofClassesOrEmpty(
				@Nullable Pattern pattern, @Nullable Predicate<AccessFlags> access,
				EntryRemapper remapper, EntryIndex entryIndex
		) {
			if (pattern == null) {
				if (access == null) {
					return Optional.empty();
				} else {
					return Optional.of(ofAccess(access, ResultType.CLASS, remapper, entryIndex));
				}
			} else {
				if (access == null) {
					return Optional.of(ofName(pattern, ResultType.CLASS, remapper));
				} else {
					return Optional.of(ofNameAndAccess(pattern, access, ResultType.CLASS, remapper, entryIndex));
				}
			}
		}

		static <E extends Entry<?>> Optional<Finder<E>> ofOrEmpty(
				@Nullable Pattern pattern, @Nullable Pattern typePattern, @Nullable Predicate<AccessFlags> access,
				ResultType resultType, EntryRemapper remapper, EntryIndex entryIndex,
				Function<E, TypeDescriptor> descriptorGetter
		) {
			if (pattern == null) {
				if (typePattern == null) {
					if (access == null) {
						return Optional.empty();
					} else {
						return Optional.of(ofAccess(access, resultType, remapper, entryIndex));
					}
				} else {
					if (access == null) {
						return Optional.of(ofType(typePattern, resultType, remapper, descriptorGetter));
					} else {
						return Optional.of(ofTypeAndAccess(typePattern, access, resultType, remapper, entryIndex, descriptorGetter));
					}
				}
			} else if (typePattern == null) {
				if (access == null) {
					return Optional.of(ofName(pattern, resultType, remapper));
				} else {
					return Optional.of(ofNameAndAccess(pattern, access, resultType, remapper, entryIndex));
				}
			} else {
				if (access == null) {
					return Optional.of(ofNameAndType(pattern, typePattern, resultType, remapper, descriptorGetter));
				} else {
					return Optional.of(ofFull(
							pattern, typePattern, access, resultType, remapper, entryIndex, descriptorGetter
					));
				}
			}
		}

		static <E extends Entry<?>> Finder<E> ofName(
				Pattern pattern, ResultType resultType, EntryRemapper remapper
		) {
			return obf -> {
				if (isMapped(remapper, obf)) {
					final E deobf = remapper.deobfuscate(obf);
					if (pattern.matcher(deobf.getName()).find()) {
						return Optional.of(Map.entry(resultType, obf));
					}
				}

				return Optional.empty();
			};
		}

		static <E extends Entry<?>> Finder<E> ofType(
				Pattern pattern, ResultType resultType, EntryRemapper remapper, Function<E, TypeDescriptor> descriptorGetter
		) {
			return obf -> {
				if (isMapped(remapper, obf) && typeMatches(obf, pattern, remapper, descriptorGetter)) {
					return Optional.of(Map.entry(resultType, obf));
				} else {
					return Optional.empty();
				}
			};
		}

		static <E extends Entry<?>> Finder<E> ofNameAndType(
				Pattern pattern, Pattern typePattern, ResultType resultType,
				EntryRemapper remapper, Function<E, TypeDescriptor> descriptorGetter
		) {
			return obf -> {
				if (isMapped(remapper, obf) && typeMatches(obf, typePattern, remapper, descriptorGetter)) {
					final E deobf = remapper.deobfuscate(obf);
					if (pattern.matcher(deobf.getName()).find()) {
						return Optional.of(Map.entry(resultType, obf));
					}
				}

				return Optional.empty();
			};
		}

		static <E extends Entry<?>> Finder<E> ofAccess(
				Predicate<AccessFlags> access, ResultType resultType, EntryRemapper remapper, EntryIndex entryIndex
		) {
			return obf -> {
				if (isMapped(remapper, obf) && accessMatches(obf, access, entryIndex)) {
					return Optional.of(Map.entry(resultType, obf));
				} else {
					return Optional.empty();
				}
			};
		}

		static <E extends Entry<?>> Finder<E> ofNameAndAccess(
				Pattern pattern, Predicate<AccessFlags> access, ResultType resultType,
				EntryRemapper remapper, EntryIndex entryIndex
		) {
			return obf -> {
				if (isMapped(remapper, obf) && accessMatches(obf, access, entryIndex)) {
					final E deobf = remapper.deobfuscate(obf);
					if (pattern.matcher(deobf.getName()).find()) {
						return Optional.of(Map.entry(resultType, obf));
					}
				}

				return Optional.empty();
			};
		}

		static <E extends Entry<?>> Finder<E> ofTypeAndAccess(
				Pattern pattern, Predicate<AccessFlags> access, ResultType resultType,
				EntryRemapper remapper, EntryIndex entryIndex, Function<E, TypeDescriptor> descriptorGetter
		) {
			return obf -> {
				if (
						isMapped(remapper, obf)
							&& typeMatches(obf, pattern, remapper, descriptorGetter)
							&& accessMatches(obf, access, entryIndex)
				) {
					return Optional.of(Map.entry(resultType, obf));
				} else {
					return Optional.empty();
				}
			};
		}

		static <E extends Entry<?>> Finder<E> ofFull(
				Pattern pattern, Pattern typePattern, Predicate<AccessFlags> access, ResultType resultType,
				EntryRemapper remapper, EntryIndex entryIndex, Function<E, TypeDescriptor> descriptorGetter
		) {
			return obf -> {
				if (
						isMapped(remapper, obf)
							&& typeMatches(obf, typePattern, remapper, descriptorGetter)
							&& accessMatches(obf, access, entryIndex)
				) {
					final E deobf = remapper.deobfuscate(obf);
					if (pattern.matcher(deobf.getName()).find()) {
						return Optional.of(Map.entry(resultType, obf));
					}
				}

				return Optional.empty();
			};
		}

		private static boolean isMapped(EntryRemapper remapper, Entry<?> entry) {
			return remapper.getMapping(entry).targetName() != null;
		}

		private static <E extends Entry<?>> boolean typeMatches(
				E entry, Pattern pattern, EntryRemapper remapper, Function<E, TypeDescriptor> descriptorGetter
		) {
			final TypeDescriptor desc = descriptorGetter.apply(entry);
			return pattern.matcher(desc.toString()).find() || pattern.matcher(getTypeName(desc, remapper)).find();
		}

		private static boolean accessMatches(
				Entry<?> entry, Predicate<AccessFlags> predicate, EntryIndex entryIndex
		) {
			final AccessFlags entryAccess = entryIndex.getEntryAccess(entry);
			return entryAccess != null && predicate.test(entryAccess);
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

	public enum Sort {
		NAME(QualifiedName.NAMES_ALPHABETIZER.thenComparing(QualifiedName.PACKAGES_ALPHABETIZER)),
		PACKAGE(QualifiedName.PACKAGES_ALPHABETIZER.thenComparing(QualifiedName.NAMES_ALPHABETIZER)),
		DEPTH(
				QualifiedName.PACKAGES_DEPTH_SORTER
					.thenComparing(QualifiedName.PACKAGES_ALPHABETIZER)
					.thenComparing(QualifiedName.NAMES_ALPHABETIZER)
		);

		private static final Sort DEFAULT = NAME;

		private final Comparator<QualifiedName> comparator;

		Sort(Comparator<QualifiedName> comparator) {
			this.comparator = comparator;
		}
	}

	private enum Access {
		PUBLIC(Opcodes.ACC_PUBLIC),
		PRIVATE(Opcodes.ACC_PRIVATE),
		PROTECTED(Opcodes.ACC_PROTECTED),
		STATIC(Opcodes.ACC_STATIC),
		FINAL(Opcodes.ACC_FINAL),
		SUPER(Opcodes.ACC_SUPER),
		SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED),
		OPEN(Opcodes.ACC_OPEN),
		TRANSITIVE(Opcodes.ACC_TRANSITIVE),
		VOLATILE(Opcodes.ACC_VOLATILE),
		BRIDGE(Opcodes.ACC_BRIDGE),
		STATIC_PHASE(Opcodes.ACC_STATIC_PHASE),
		VARARGS(Opcodes.ACC_VARARGS),
		TRANSIENT(Opcodes.ACC_TRANSIENT),
		NATIVE(Opcodes.ACC_NATIVE),
		INTERFACE(Opcodes.ACC_INTERFACE),
		ABSTRACT(Opcodes.ACC_ABSTRACT),
		STRICT(Opcodes.ACC_STRICT),
		SYNTHETIC(Opcodes.ACC_SYNTHETIC),
		ANNOTATION(Opcodes.ACC_ANNOTATION),
		ENUM(Opcodes.ACC_ENUM),
		MANDATED(Opcodes.ACC_MANDATED),
		MODULE(Opcodes.ACC_MODULE),
		RECORD(Opcodes.ACC_RECORD),
		DEPRECATED(Opcodes.ACC_DEPRECATED);

		final int flag;
		final String lowercase;

		Access(int flag) {
			this.flag = flag;
			this.lowercase = this.name().toLowerCase();
		}

		@Override
		public String toString() {
			return this.lowercase;
		}
	}

	record Required(Path inputJar, Path inputMappings) { }
	record Optionals(
			Pattern classes, Predicate<AccessFlags> classAccess,
			Pattern methods, Pattern methodReturns, Predicate<AccessFlags> methodAccess,
			Pattern fields, Pattern fieldTypes, Predicate<AccessFlags> fieldAccess,
			Pattern params, Pattern paramTypes, Predicate<AccessFlags> paramAccess,
			Sort sort, Integer limit
	) { }
}

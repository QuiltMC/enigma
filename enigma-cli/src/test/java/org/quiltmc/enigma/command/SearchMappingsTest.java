package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.command.SearchMappingsCommand.ResultType;
import org.quiltmc.enigma.command.SearchMappingsCommand.Sort;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.alwaysTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quiltmc.enigma.TestUtil.getResource;

public class SearchMappingsTest {
	private static final Path JAR = TestUtil.obfJar("complete");
	private static final Path MAPPINGS = getResource("/search_mappings");

	// classes
	private static final String INNER_CLASS = "InnerClass";
	private static final String OUTER_CLASS = "OuterClass";
	private static final String KEEP_PACKAGED = "KeepPackaged";
	private static final String PACKAGED_CLASS = "PackagedClass";
	private static final String OTHER_RETURN_INTERFACE = "OtherReturnInterface";
	private static final String INNER_FIELD_TYPE = "InnerFieldType";
	private static final String PARAM_TYPE = "ParamType";
	private static final String SELF_RETURN_ENUM = "SelfReturnEnum";
	// methods
	private static final String INT_TO_VOID_METHOD = "intToVoidMethod";
	private static final String VOID_METHOD = "voidMethod";
	private static final String INT_METHOD = "intMethod";
	private static final String INT_TO_INT_METHOD = "intToIntMethod";
	private static final String GET_OTHER = "getOther";
	private static final String ABSTRACT_METHOD = "abstractMethod";
	private static final String STATIC_GET_ARRAY = "staticGetArray";
	private static final String STATIC_GET = "staticGet";
	private static final String STATIC_VOID_METHOD = "staticVoidMethod";
	// fields
	private static final String INT_FIELD = "intField";
	private static final String FLOAT_FIELD = "floatField";
	private static final String STRING_FIELD = "stringField";
	private static final String PRIVATE_STRING_FIELD = "privateStringField";
	private static final String PRIVATE_STATIC_FINAL_INT_FIELD = "PRIVATE_STATIC_FINAL_INT_FIELD";
	// params
	private static final String STATIC_TYPED_PARAM = "staticTypedParam";
	private static final String STATIC_STRING_PARAM = "staticStringParam";
	private static final String INT_PARAM = "intParam";
	private static final String CONSTRUCTOR_INT_PARAM = "constructorIntParam";
	private static final String CONSTRUCTOR_PARAM_STRING = "constructorParamString";
	// record components
	// only their fields are mapped, not getters or canonical constructor args
	private static final String RECORD_INT = "recordInt";
	private static final String RECORD_STRING = "recordString";
	private static final String RECORD_STRING_2 = "recordString2";

	private static final ImmutableList<String> CLASS_NAMES = ImmutableList.of(
			OUTER_CLASS,
			INNER_CLASS,
			KEEP_PACKAGED,
			PACKAGED_CLASS,
			OTHER_RETURN_INTERFACE,
			INNER_FIELD_TYPE,
			PARAM_TYPE,
			SELF_RETURN_ENUM
	);

	private static final ImmutableList<String> METHOD_NAMES = ImmutableList.of(
			INT_TO_VOID_METHOD,
			VOID_METHOD,
			INT_METHOD,
			INT_TO_INT_METHOD,
			GET_OTHER,
			ABSTRACT_METHOD,
			STATIC_GET_ARRAY,
			STATIC_GET,
			STATIC_VOID_METHOD
	);

	private static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(
			INT_FIELD,
			FLOAT_FIELD,
			STRING_FIELD,
			PRIVATE_STRING_FIELD,
			PRIVATE_STATIC_FINAL_INT_FIELD,
			RECORD_INT,
			RECORD_STRING,
			RECORD_STRING_2
	);

	// Does not include RECORD_INT or RECORD_STRING because EntryIndex doesn't see canonical record constructors.
	// They can be found through their fields or getters instead.
	private static final ImmutableList<String> PARAM_NAMES = ImmutableList.of(
			STATIC_TYPED_PARAM,
			STATIC_STRING_PARAM,
			INT_PARAM,
			CONSTRUCTOR_INT_PARAM,
			CONSTRUCTOR_PARAM_STRING
	);

	@Test
	void findsClassNames() {
		final String found = runDefault(
				Pattern.compile("erClass$"), null,
				null, null, null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.CLASS, INNER_CLASS, OUTER_CLASS);
	}

	@Test
	void findsAccessedClasses() {
		final String found = runDefault(
				null, AccessFlags::isEnum,
				null, null, null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.CLASS, SELF_RETURN_ENUM);
	}

	@Test
	void findsAccessFilteredClassNames() {
		final String found = runDefault(
				Pattern.compile("Return"), access -> !access.isEnum(),
				null, null, null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.CLASS, OTHER_RETURN_INTERFACE);
	}

	@Test
	void findsMethodNames() {
		final String found = runDefault(
				null, null,
				Pattern.compile("Method$"), null, null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(
				found, ResultType.METHOD,
				INT_TO_VOID_METHOD, VOID_METHOD, INT_METHOD, INT_TO_INT_METHOD, ABSTRACT_METHOD, STATIC_VOID_METHOD
		);
	}

	@Test
	void findsVoidMethods() {
		final String found = runDefault(
				null, null,
				null, Pattern.compile("^void$"), null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, VOID_METHOD, INT_TO_VOID_METHOD, STATIC_VOID_METHOD);
	}

	@Test
	void findsPrimitiveMethods() {
		final String found = runDefault(
				null, null,
				null, Pattern.compile("^int"), null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, INT_METHOD, INT_TO_INT_METHOD);
	}

	@Test
	void findsTypedMethods() {
		final String found = runDefault(
				null, null,
				null, Pattern.compile("^OtherReturnInterface$"), null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, GET_OTHER);
	}

	@Test
	void findsAccessedMethods() {
		final String found = runDefault(
				null, null,
				null, null, AccessFlags::isAbstract,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, ABSTRACT_METHOD);
	}

	@Test
	void findsTypeFilteredMethodNames() {
		final String found = runDefault(
				null, null,
				Pattern.compile("^intTo"), Pattern.compile("^void$"), null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, INT_TO_VOID_METHOD);
	}

	@Test
	void findsAccessFilteredMethodNames() {
		final String found = runDefault(
				null, null,
				Pattern.compile(".*(?<!Array)$"), null, AccessFlags::isStatic,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, STATIC_GET, STATIC_VOID_METHOD);
	}

	@Test
	void findsFullyFilteredMethodNames() {
		final String found = runDefault(
				null, null,
				Pattern.compile("^(?!int)"), Pattern.compile("^void$"), access -> !access.isStatic(),
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, VOID_METHOD);
	}

	@Test
	void findsFieldNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				Pattern.compile("Field$"), null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, INT_FIELD, FLOAT_FIELD, STRING_FIELD, PRIVATE_STRING_FIELD);
	}

	@Test
	void findsPrimitiveFields() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, Pattern.compile("^int$"), null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, INT_FIELD, PRIVATE_STATIC_FINAL_INT_FIELD, RECORD_INT);
	}

	@Test
	void findsTypedFields() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, Pattern.compile("^java\\.lang\\.String$"), null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, STRING_FIELD, PRIVATE_STRING_FIELD, RECORD_STRING, RECORD_STRING_2);
	}

	@Test
	void findsAccessedFields() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, AccessFlags::isPrivate,
				null, null, null
		);

		assertOnlyResults(
				found, ResultType.FIELD,
				PRIVATE_STRING_FIELD, PRIVATE_STATIC_FINAL_INT_FIELD, RECORD_INT, RECORD_STRING, RECORD_STRING_2
		);
	}

	@Test
	void findsTypeFilteredFieldNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				Pattern.compile("Field$"), Pattern.compile("^float$"), null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, FLOAT_FIELD);
	}

	@Test
	void findsAccessFilteredFieldNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				Pattern.compile("(?i)int"), null, AccessFlags::isPrivate,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, PRIVATE_STATIC_FINAL_INT_FIELD, RECORD_INT);
	}

	@Test
	void findsFullyFilteredFieldNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				Pattern.compile(".*(?<!2)$"), Pattern.compile("^java\\.lang\\.String$"), AccessFlags::isPrivate,
				null, null, null
		);

		assertOnlyResults(found, ResultType.FIELD, RECORD_STRING, PRIVATE_STRING_FIELD);
	}

	@Test
	void findsParamNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("Param$"), null, null
		);

		assertOnlyResults(
				found, ResultType.PARAM,
				STATIC_TYPED_PARAM, STATIC_STRING_PARAM, INT_PARAM, CONSTRUCTOR_INT_PARAM
		);
	}

	@Test
	void findsPrimitiveParams() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				null, Pattern.compile("^int$"), null
		);

		assertOnlyResults(found, ResultType.PARAM, INT_PARAM, CONSTRUCTOR_INT_PARAM);
	}

	@Test
	void findsTypedParams() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				null, Pattern.compile("^ParamType$"), null
		);

		assertOnlyResults(found, ResultType.PARAM, STATIC_TYPED_PARAM);
	}

	@Test
	void findsAccessedParams() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				null, null, AccessFlags::isStatic
		);

		assertOnlyResults(found, ResultType.PARAM, STATIC_TYPED_PARAM, STATIC_STRING_PARAM);
	}

	@Test
	void findsTypeFilteredParamNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("^constructor"), Pattern.compile("^java\\.lang\\.String$"), null
		);

		assertOnlyResults(found, ResultType.PARAM, CONSTRUCTOR_PARAM_STRING);
	}

	@Test
	void findsAccessFilteredParamNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("st"), null, access -> !access.isStatic()
		);

		assertOnlyResults(found, ResultType.PARAM, CONSTRUCTOR_INT_PARAM, CONSTRUCTOR_PARAM_STRING);
	}

	@Test
	void findsFullyFilteredParamNames() {
		final String found = runDefault(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("st"), Pattern.compile("^.*(?<!String)$"), access -> !access.isStatic()
		);

		assertOnlyResults(found, ResultType.PARAM, CONSTRUCTOR_INT_PARAM);
	}

	@Test
	void findsArrayTypes() {
		final String found = runDefault(
				null, null,
				null, Pattern.compile("\\[\\]"), null,
				null, null, null,
				null, null, null
		);

		assertOnlyResults(found, ResultType.METHOD, STATIC_GET_ARRAY);
	}

	@Test
	void nameSorts() {
		final String found = run(
				Pattern.compile(".*"), null,
				null, null, null,
				null, null, null,
				null, null, null,
				Sort.NAME, -1
		);

		assertResultOrder(found, ResultType.CLASS, KEEP_PACKAGED, PACKAGED_CLASS, SELF_RETURN_ENUM);
	}

	@Test
	void packageSorts() {
		final String found = run(
				Pattern.compile(".*"), null,
				null, null, null,
				null, null, null,
				null, null, null,
				Sort.PACKAGE, -1
		);

		assertResultOrder(found, ResultType.CLASS, SELF_RETURN_ENUM, KEEP_PACKAGED, PACKAGED_CLASS);
	}

	@Test
	void depthSorts() {
		final String found = run(
				Pattern.compile(".*"), null,
				null, null, null,
				null, null, null,
				null, null, null,
				Sort.DEPTH, -1
		);

		assertResultOrder(found, ResultType.CLASS, SELF_RETURN_ENUM, PACKAGED_CLASS, KEEP_PACKAGED);
	}

	@Test
	void findsEverythingAndLimitable() {
		final Pattern anything = Pattern.compile(".*");
		final String found = runDefault(
				anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue()
		);

		final String limitedFound = runNonEmpty(
				anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				null, 0
		);

		for (final ResultType type : ResultType.values()) {
			final String[] names = getNames(type).toArray(String[]::new);
			assertOnlyResultsOfType(found, type, names);

			assertResultCount(limitedFound, names.length, type);
			for (final String name : names) {
				assertLacks(limitedFound, name);
			}
		}
	}

	@Test
	void findsNothing() {
		final Pattern unmatchable = Pattern.compile(" ");
		final String unmatchableArgsFound = run(
				unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				null, -1
		);

		assertTrue(unmatchableArgsFound.isEmpty(), "Expected to find empty string!");

		final String noArgsFound = run(
				null, null,
				null, null, null,
				null, null, null,
				null, null, null,
				null, -1
		);

		assertTrue(noArgsFound.isEmpty(), "Expected to find empty string!");
	}

	/**
	 * Uses default sort with no limit and verifies non-empty.
	 */
	private static String runDefault(
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramTypes, @Nullable Predicate<AccessFlags> paramAccess
	) {
		return runNonEmpty(
			classes, classAccess,
			methods, methodReturns, methodAccess,
			fields, fieldTypes, fieldAccess,
			params, paramTypes, paramAccess,
			null, -1
		);
	}

	private static String runNonEmpty(
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramTypes, @Nullable Predicate<AccessFlags> paramAccess,
			@Nullable Sort sort, int limit
	) {
		final String found = run(
				classes, classAccess,
				methods, methodReturns, methodAccess,
				fields, fieldTypes, fieldAccess,
				params, paramTypes, paramAccess,
				sort, limit
		);

		assertFalse(found.isEmpty(), "Unexpected empty result!");
		// log for manual confirmation of formatting
		Logger.info(found);

		return found;
	}

	private static String run(
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramTypes, @Nullable Predicate<AccessFlags> paramAccess,
			@Nullable Sort sort, int limit
	) {
		try {
			return SearchMappingsCommand.runImpl(
					JAR, MAPPINGS,
					classes, classAccess,
					methods, methodReturns, methodAccess,
					fields, fieldTypes, fieldAccess,
					params, paramTypes, paramAccess,
					sort, limit
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({"unused", "RedundantThrows"})
	private static void assertResultOrder(String found, ResultType type) throws Throwable {
		throw new UnsupportedOperationException();
	}

	private static void assertResultOrder(String found, ResultType type, String... words) {
		final String typeSection = requireResultTypeSection(found, type);

		int prev = requireIndexOf(typeSection, words[0]);

		for (int n = 1; n < words.length; n++) {
			final String word = words[n];
			final int current = requireIndexOf(typeSection, word);

			final String prevWord = words[n - 1];
			assertTrue(prev < current, () -> getExpectedBeforeMessage(prevWord, word));

			prev = current;
		}
	}

	@SuppressWarnings({"unused", "RedundantThrows"})
	private static void assertOnlyResults(String found, ResultType type) throws Throwable {
		throw new IllegalArgumentException("No expected names specified!");
	}

	/**
	 * Asserts that the passed {@code expectedNames} are the only results in the passed {@code found} string.
	 */
	private static void assertOnlyResults(String found, ResultType type, String... expectedNames) {
		assertOnlyResultCount(found, expectedNames.length, type);
		assertOnlyResultsOfType(found, type, expectedNames);
	}

	/**
	 * Asserts that the passed {@code expectedNames} are the only results of the passed {@code type} in the passed
	 * {@code found} string.
	 */
	private static void assertOnlyResultsOfType(String found, ResultType type, String... expectedNames) {
		final String header = type.buildResultHeader(new StringBuilder(), expectedNames.length).toString();
		final int headerStart = requireIndexOf(found, header);
		final String typeSection = getResultTypeSection(found, headerStart + header.length());

		final Set<String> expected = Set.of(expectedNames);
		for (final String name : getNames(type)) {
			if (expected.contains(name)) {
				assertContains(typeSection, name);
			} else {
				assertLacks(typeSection, name);
			}
		}
	}

	private static ImmutableList<String> getNames(ResultType type) {
		return switch (type) {
			case CLASS -> CLASS_NAMES;
			case METHOD -> METHOD_NAMES;
			case FIELD -> FIELD_NAMES;
			case PARAM -> PARAM_NAMES;
		};
	}

	private static void assertOnlyResultCount(String found, int count, ResultType type) {
		for (final ResultType value : ResultType.values()) {
			if (value == type) {
				assertResultCount(found, count, type);
			} else {
				assertNoResults(found, value);
			}
		}
	}

	private static void assertResultCount(String found, int count, ResultType type) {
		assertContains(found, type.buildResultHeader(new StringBuilder(), count).toString());
	}

	private static void assertNoResults(String found, ResultType... types) {
		for (final ResultType type : types) {
			assertFalse(
					resultHeaderPatternOf(type).matcher(found).find(),
					() -> "Unexpected result type: " + type
			);
		}
	}

	private static String requireResultTypeSection(String found, ResultType type) {
		final Matcher headerMatcher = resultHeaderPatternOf(type).matcher(found);
		assertTrue(headerMatcher.find(), () -> "Expected '%s' to contain '%s' results!".formatted(found, type));
		return getResultTypeSection(found, headerMatcher.end());
	}

	private static String getResultTypeSection(String found, int headerEnd) {
		final Matcher nextResultMatcher = Pattern.compile("Found \\d+ \\w+").matcher(found);
		final int typeSectionEnd = nextResultMatcher.find(headerEnd) ? nextResultMatcher.start() : found.length();
		return found.substring(headerEnd, typeSectionEnd);
	}

	private static int requireIndexOf(String string, String word) {
		final Matcher matcher = wordPatternOf(word).matcher(string);

		assertTrue(matcher.find(), () -> getExpectedToContainMessage(string, word));

		return matcher.start();
	}

	private static void assertContains(String string, String word) {
		assertTrue(containsWord(string, word), () -> getExpectedToContainMessage(string, word));
	}

	private static void assertLacks(String string, String word) {
		assertFalse(containsWord(string, word), () -> "Did not expect '%s' to contain '%s'!".formatted(string, word));
	}

	private static boolean containsWord(String string, String word) {
		return wordPatternOf("\\b" + word + "\\b").matcher(string).find();
	}

	private static Pattern resultHeaderPatternOf(ResultType type) {
		return Pattern.compile("Found \\d+ (?:%s|%s)".formatted(type.singleName, type.pluralName));
	}

	private static Pattern wordPatternOf(String word) {
		return Pattern.compile(word);
	}

	private static String getExpectedToContainMessage(String string, String word) {
		return "Expected '%s' to contain '%s'!".formatted(string, word);
	}

	private static String getExpectedBeforeMessage(String expectedFirst, String expectedSecond) {
		return "Expected '%s' to come before '%s'!".formatted(expectedFirst, expectedSecond);
	}
}

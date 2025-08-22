package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.command.GrepMappingsCommand.ResultType;
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

public class GrepMappingsTest {
	private static final Path JAR = TestUtil.obfJar("complete");
	private static final Path MAPPINGS = getResource("/grep_mappings");

	// classes
	private static final String INNER_CLASS = "InnerClass";
	private static final String OUTER_CLASS = "OuterClass";
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
	// fields
	private static final String INT_FIELD = "intField";
	private static final String FLOAT_FIELD = "floatField";
	private static final String STRING_FIELD = "stringField";
	private static final String PRIVATE_STATIC_FINAL_INT_FIELD = "PRIVATE_STATIC_FINAL_INT_FIELD";
	// params
	private static final String STATIC_TYPED_PARAM = "staticTypedParam";
	private static final String STRING_PARAM = "stringParam";
	private static final String INT_PARAM = "intParam";
	private static final String CONSTRUCTOR_INT_PARAM = "constructorIntParam";
	private static final String CONSTRUCTOR_PARAM_STRING = "constructorParamString";
	// record components
	// only their fields are mapped, not getters or canonical constructor args
	private static final String RECORD_INT = "recordInt";
	private static final String RECORD_STRING = "recordString";

	private static final ImmutableList<String> CLASS_NAMES = ImmutableList.of(
			OUTER_CLASS,
			INNER_CLASS,
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
			STATIC_GET
	);

	private static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(
			INT_FIELD,
			FLOAT_FIELD,
			STRING_FIELD,
			PRIVATE_STATIC_FINAL_INT_FIELD,
			RECORD_INT,
			RECORD_STRING
	);

	// Does not include RECORD_INT or RECORD_STRING because EntryIndex doesn't see canonical record constructors.
	// They can be found through their fields or getters instead.
	private static final ImmutableList<String> PARAM_NAMES = ImmutableList.of(
			STATIC_TYPED_PARAM,
			STRING_PARAM,
			INT_PARAM,
			CONSTRUCTOR_INT_PARAM,
			CONSTRUCTOR_PARAM_STRING
	);

	@Test
	void findsClassNames() {
		final String found = runNonEmpty(
				Pattern.compile("erClass$"), null,
				null, null, null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.CLASS, INNER_CLASS, OUTER_CLASS);
	}

	@Test
	void findsMethodNames() {
		final String found = runNonEmpty(
				null, null,
				Pattern.compile("Method$"), null, null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(
				found, ResultType.METHOD,
				INT_TO_VOID_METHOD, VOID_METHOD, INT_METHOD, INT_TO_INT_METHOD, ABSTRACT_METHOD
		);
	}

	@Test
	void findsVoidMethods() {
		final String found = runNonEmpty(
				null, null,
				null, Pattern.compile("^void$"), null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.METHOD, VOID_METHOD, INT_TO_VOID_METHOD);
	}

	@Test
	void findsPrimitiveMethods() {
		final String found = runNonEmpty(
				null, null,
				null, Pattern.compile("^int"), null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.METHOD, INT_METHOD, INT_TO_INT_METHOD);
	}

	@Test
	void findsTypedMethods() {
		final String found = runNonEmpty(
				null, null,
				null, Pattern.compile("^OtherReturnInterface$"), null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.METHOD, GET_OTHER);
	}

	@Test
	void findsTypeFilteredMethodNames() {
		final String found = runNonEmpty(
				null, null,
				Pattern.compile("^intTo"), Pattern.compile("^void$"), null,
				null, null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.METHOD, INT_TO_VOID_METHOD);
	}

	@Test
	void findsFieldNames() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				Pattern.compile("Field$"), null, null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.FIELD, INT_FIELD, FLOAT_FIELD, STRING_FIELD);
	}

	@Test
	void findsPrimitiveFields() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, Pattern.compile("^int$"), null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.FIELD, INT_FIELD, PRIVATE_STATIC_FINAL_INT_FIELD, RECORD_INT);
	}

	@Test
	void findsTypedFields() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, Pattern.compile("^java\\.lang\\.String$"), null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.FIELD, STRING_FIELD, RECORD_STRING);
	}

	@Test
	void findsTypeFilteredFieldNames() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				Pattern.compile("Field$"), Pattern.compile("^float$"), null,
				null, null, null,
				-1
		);

		assertOnlyResults(found, ResultType.FIELD, FLOAT_FIELD);
	}

	@Test
	void findsParamNames() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("Param$"), null, null,
				-1
		);

		assertOnlyResults(found, ResultType.PARAM, STATIC_TYPED_PARAM, STRING_PARAM, INT_PARAM, CONSTRUCTOR_INT_PARAM);
	}

	@Test
	void findsPrimitiveParams() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, null, null,
				null, Pattern.compile("^int$"), null,
				-1
		);

		assertOnlyResults(found, ResultType.PARAM, INT_PARAM, CONSTRUCTOR_INT_PARAM);
	}

	@Test
	void findsTypedParams() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, null, null,
				null, Pattern.compile("^ParamType$"), null,
				-1
		);

		assertOnlyResults(found, ResultType.PARAM, STATIC_TYPED_PARAM);
	}

	@Test
	void findsTypeFilteredParamNames() {
		final String found = runNonEmpty(
				null, null,
				null, null, null,
				null, null, null,
				Pattern.compile("^constructor"), Pattern.compile("^java\\.lang\\.String$"), null,
				-1
		);

		assertOnlyResults(found, ResultType.PARAM, CONSTRUCTOR_PARAM_STRING);
	}

	@Test
	void findsEverythingAndLimitable() {
		final Pattern anything = Pattern.compile(".*");
		final String found = runNonEmpty(
				anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				-1
		);

		final String limitedFound = runNonEmpty(
				anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				anything, anything, alwaysTrue(),
				0
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
		final String found = run(
				unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				unmatchable, unmatchable, alwaysFalse(),
				-1
		);

		assertTrue(found.isEmpty(), "Expected to find empty string!");
	}

	private static String runNonEmpty(
			@Nullable Pattern classes, @Nullable Predicate<AccessFlags> classAccess,
			@Nullable Pattern methods, @Nullable Pattern methodReturns, @Nullable Predicate<AccessFlags> methodAccess,
			@Nullable Pattern fields, @Nullable Pattern fieldTypes, @Nullable Predicate<AccessFlags> fieldAccess,
			@Nullable Pattern params, @Nullable Pattern paramTypes, @Nullable Predicate<AccessFlags> paramAccess,
			int limit
	) {
		final String found = run(
				classes, classAccess,
				methods, methodReturns, methodAccess,
				fields, fieldTypes, fieldAccess,
				params, paramTypes, paramAccess,
				limit
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
			int limit
	) {
		try {
			return GrepMappingsCommand.runImpl(
					JAR, MAPPINGS,
					classes, classAccess,
					methods, methodReturns, methodAccess,
					fields, fieldTypes, fieldAccess,
					params, paramTypes, paramAccess,
					limit
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
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
		final ImmutableList<String> names = getNames(type);

		final String header = type.buildResultHeader(new StringBuilder(), expectedNames.length).toString();
		final int headerStart = found.indexOf(header);
		assertFalse(headerStart < 0, () -> getExpectedToContainMessage(found, header));

		final int headerEnd = headerStart + header.length();
		final Matcher nextResultMatcher = Pattern.compile("Found \\d+ \\w+").matcher(found);
		final int typeSectionEnd = nextResultMatcher.find(headerEnd) ? nextResultMatcher.start() : found.length();
		final String typeSection = found.substring(headerEnd, typeSectionEnd);

		final Set<String> expected = Set.of(expectedNames);
		for (final String name : names) {
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
			final Pattern resultHeaderPattern = Pattern
					.compile("Found \\d+ (?:%s|%s)".formatted(type.singleName, type.pluralName));
			assertFalse(
					resultHeaderPattern.matcher(found).find(),
					() -> "Unexpected result type: " + type
			);
		}
	}

	private static void assertContains(String string, String part) {
		assertTrue(string.contains(part), () -> getExpectedToContainMessage(string, part));
	}

	private static void assertLacks(String string, String part) {
		assertFalse(string.contains(part), () -> "Did not expect '%s' to contain '%s'!".formatted(string, part));
	}

	private static String getExpectedToContainMessage(String string, String part) {
		return "Expected '%s' to contain '%s'!".formatted(string, part);
	}
}

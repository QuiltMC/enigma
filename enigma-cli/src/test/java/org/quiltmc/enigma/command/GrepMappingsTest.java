package org.quiltmc.enigma.command;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.command.GrepMappingsCommand.ResultType;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quiltmc.enigma.TestUtil.getResource;

public class GrepMappingsTest {
	private static final Path JAR = TestUtil.obfJar("complete");
	private static final Path MAPPINGS = getResource("/grep_mappings");

	private static final String INNER_CLASS = "InnerClass";
	private static final String OUTER_CLASS = "OuterClass";

	private static final String INT_TO_VOID_METHOD = "intToVoidMethod";
	private static final String VOID_METHOD = "voidMethod";
	private static final String INT_METHOD = "intMethod";
	private static final String INT_TO_INT_METHOD = "intToIntMethod";
	private static final String GET_OTHER = "getOther";

	@Test
	void findsClassNames() {
		final String found = runNonEmpty(
				Pattern.compile("erClass$"),
				null, null, null, null, null, null, -1
		);

		assertResultCount(found, 2, ResultType.CLASS);
		assertContains(found, INNER_CLASS);
		assertContains(found, OUTER_CLASS);
	}

	@Test
	void findsMethodNames() {
		final String found = runNonEmpty(
				null,
				Pattern.compile("Method$"),
				null, null, null, null, null, -1
		);

		assertResultCount(found, 4, ResultType.METHOD);
		assertContains(found, INT_TO_VOID_METHOD);
		assertContains(found, VOID_METHOD);
		assertContains(found, INT_METHOD);
		assertContains(found, INT_TO_INT_METHOD);
	}

	@Test
	void findsVoidMethods() {
		final String found = runNonEmpty(
				null, null, null, null,
				Pattern.compile("^void$"),
				null, null, -1
		);

		assertResultCount(found, 2, ResultType.METHOD);
		assertContains(found, VOID_METHOD);
		assertContains(found, INT_TO_VOID_METHOD);
	}

	@Test
	void findsPrimitiveMethods() {
		final String found = runNonEmpty(
				null, null, null, null,
				Pattern.compile("^int"),
				null, null, -1
		);

		assertResultCount(found, 2, ResultType.METHOD);
		assertContains(found, INT_METHOD);
		assertContains(found, INT_TO_INT_METHOD);
	}

	@Test
	void findsTypedMethods() {
		final String found = runNonEmpty(
				null, null, null, null,
				Pattern.compile("^OtherReturnType$"),
				null, null, -1
		);

		assertResultCount(found, 1, ResultType.METHOD);
		assertContains(found, GET_OTHER);
	}

	@Test
	void findsMethodNamesFiltered() {
		final String found = runNonEmpty(
				null,
				Pattern.compile("^intTo"),
				null, null,
				Pattern.compile("^void$"),
				null, null, -1
		);

		assertResultCount(found, 1, ResultType.METHOD);
		assertContains(found, INT_TO_VOID_METHOD);
		assertLacks(found, VOID_METHOD);
	}

	private static String runNonEmpty(
			@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern parameters,
			@Nullable Pattern methodsReturns, @Nullable Pattern fieldTypes, @Nullable Pattern paramTypes, int limit
	) {
		final String found = run(classes, methods, fields, parameters, methodsReturns, fieldTypes, paramTypes, limit);

		assertFalse(found.isEmpty());
		Logger.info(found);

		return found;
	}

	private static String run(
			@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern parameters,
			@Nullable Pattern methodsReturns, @Nullable Pattern fieldTypes, @Nullable Pattern paramTypes, int limit
	) {
		try {
			return GrepMappingsCommand.runImpl(
				JAR, MAPPINGS, classes, methods, fields, parameters, methodsReturns, fieldTypes, paramTypes, limit
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void assertResultCount(String found, int count, ResultType type) {
		assertContains(found, type.buildResultHeader(new StringBuilder(), count).toString());
	}

	private static void assertContains(String string, String part) {
		assertTrue(string.contains(part), () -> "Expected '%s' to contain '%s'!".formatted(string, part));
	}

	private static void assertLacks(String string, String part) {
		assertFalse(string.contains(part), () -> "Did not expect '%s' to contain '%s'!".formatted(string, part));
	}
}

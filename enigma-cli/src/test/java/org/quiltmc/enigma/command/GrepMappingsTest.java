package org.quiltmc.enigma.command;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
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

	@Test
	void findsInnerAndOuterClasses() throws Exception {
		final String found = runNonEmpty(
				Pattern.compile("erClass$"),
				null, null, null, null, null, null, -1
		);

		assertFalse(found.isEmpty());
		Logger.info(found);

		assertContains(found, "2 classes");
		assertContains(found, "InnerClass");
		assertContains(found, "OuterClass");
	}

	private static String runNonEmpty(
			@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern parameters,
			@Nullable Pattern methodsReturns, @Nullable Pattern fieldTypes, @Nullable Pattern paramTypes, int limit
	) throws Exception {
		final String found = run(classes, methods, fields, parameters, methodsReturns, fieldTypes, paramTypes, limit);

		assertFalse(found.isEmpty());
		Logger.info(found);

		return found;
	}

	private static String run(
			@Nullable Pattern classes, @Nullable Pattern methods, @Nullable Pattern fields, @Nullable Pattern parameters,
			@Nullable Pattern methodsReturns, @Nullable Pattern fieldTypes, @Nullable Pattern paramTypes, int limit
	) throws Exception {
		return GrepMappingsCommand.runImpl(
			JAR, MAPPINGS, classes, methods, fields, parameters, methodsReturns, fieldTypes, paramTypes, limit
		);
	}

	private static void assertContains(String string, String part) {
		assertTrue(string.contains(part), () -> "Expected '%s' to contain '%s'!".formatted(string, part));
	}
}

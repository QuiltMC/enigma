package org.quiltmc.enigma.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class DropInvalidMappingsTest extends CommandTest {
	private static final Path JAR = TestUtil.obfJar("lone_class");
	private static final Path INPUT_DIR = getResource("/drop_invalid_mappings/input/");
	private static final Path EXPECTED_DIR = getResource("/drop_invalid_mappings/expected/");
	private static final Path INVALID_MAPPINGS_INPUT = INPUT_DIR.resolve("InvalidMappings.mapping");
	private static final Path INVALID_MAPPINGS_EXPECTED = EXPECTED_DIR.resolve("InvalidMappings.mapping");
	private static final Path EMPTY_MAPPINGS_INPUT = INPUT_DIR.resolve("EmptyMappings.mapping");
	private static final Path EMPTY_MAPPINGS_EXPECTED = EXPECTED_DIR.resolve("EmptyMappings.mapping");

	@Test
	public void testInvalidMappings() throws Exception {
		Path resultFile = Files.createTempFile("invalidMappingsResult", ".mapping");

		DropInvalidMappingsCommand.run(JAR, INVALID_MAPPINGS_INPUT, resultFile);

		String expectedLines = Files.readString(INVALID_MAPPINGS_EXPECTED);
		String actualLines = Files.readString(resultFile);

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testEmptyMappings() throws Exception {
		Path resultFile = Files.createTempFile("emptyMappingsResult", ".mapping");

		DropInvalidMappingsCommand.run(JAR, EMPTY_MAPPINGS_INPUT, resultFile);

		String expectedLines = Files.readString(EMPTY_MAPPINGS_EXPECTED);
		String actualLines = Files.readString(resultFile);

		Assertions.assertEquals(expectedLines, actualLines);
	}
}

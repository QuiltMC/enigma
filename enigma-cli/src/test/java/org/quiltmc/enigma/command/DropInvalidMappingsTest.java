package org.quiltmc.enigma.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.quiltmc.enigma.TestUtil.toNewLineEndings;

public class DropInvalidMappingsTest extends CommandTest {
	private static final Path LONE_JAR = TestUtil.obfJar("lone_class");
	private static final Path INNER_JAR = TestUtil.obfJar("inner_classes");
	private static final Path ENUMS_JAR = TestUtil.obfJar("enums");
	private static final Path DROP_INVALID_MAPPINGS_JAR = TestUtil.obfJar("drop_invalid_mappings");
	private static final Path INPUT_DIR = getResource("/drop_invalid_mappings/input/");
	private static final Path EXPECTED_DIR = getResource("/drop_invalid_mappings/expected/");

	private static final Path INVALID_MAPPINGS_INPUT = INPUT_DIR.resolve("InvalidMappings.mapping");
	private static final Path INVALID_MAPPINGS_EXPECTED = EXPECTED_DIR.resolve("InvalidMappings.mapping");
	private static final Path EMPTY_MAPPINGS_INPUT = INPUT_DIR.resolve("EmptyMappings.mapping");
	private static final Path EMPTY_MAPPINGS_EXPECTED = EXPECTED_DIR.resolve("EmptyMappings.mapping");
	private static final Path MAPPING_SAVE_INPUT = INPUT_DIR.resolve("MappingSave.mapping");
	private static final Path MAPPING_SAVE_EXPECTED = EXPECTED_DIR.resolve("MappingSave.mapping");
	private static final Path DISCARD_INNER_CLASS_INPUT = INPUT_DIR.resolve("DiscardInnerClass.mapping");
	private static final Path DISCARD_INNER_CLASS_EXPECTED = EXPECTED_DIR.resolve("DiscardInnerClass.mapping");
	private static final Path ENUMS_INPUT = INPUT_DIR.resolve("Enums.mapping");
	private static final Path ENUMS_EXPECTED = EXPECTED_DIR.resolve("Enums.mapping");
	private static final Path PARAM_INDEXES_INPUT = INPUT_DIR.resolve("ParamIndexes.mapping");
	private static final Path PARAM_INDEXES_EXPECTED = EXPECTED_DIR.resolve("ParamIndexes.mapping");

	@Test
	public void testInvalidMappings() throws Exception {
		Path resultFile = Files.createTempFile("invalidMappingsResult", ".mapping");

		DropInvalidMappingsCommand.run(LONE_JAR, INVALID_MAPPINGS_INPUT, resultFile);

		String expectedLines = toNewLineEndings(Files.readString(INVALID_MAPPINGS_EXPECTED));
		String actualLines = toNewLineEndings(Files.readString(resultFile));

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testEmptyMappings() throws Exception {
		Path resultFile = Files.createTempFile("emptyMappingsResult", ".mapping");

		DropInvalidMappingsCommand.run(LONE_JAR, EMPTY_MAPPINGS_INPUT, resultFile);

		String expectedLines = Files.readString(EMPTY_MAPPINGS_EXPECTED);
		String actualLines = Files.readString(resultFile);

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testMappingSave() throws Exception {
		Path resultFile = Files.createTempFile("mappingSaveResult", ".mapping");

		DropInvalidMappingsCommand.run(INNER_JAR, MAPPING_SAVE_INPUT, resultFile);

		String expectedLines = toNewLineEndings(Files.readString(MAPPING_SAVE_EXPECTED));
		String actualLines = toNewLineEndings(Files.readString(resultFile));

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testDiscardInnerClass() throws Exception {
		Path resultFile = Files.createTempFile("discardInnerClass", ".mapping");

		DropInvalidMappingsCommand.run(INNER_JAR, DISCARD_INNER_CLASS_INPUT, resultFile);

		String expectedLines = toNewLineEndings(Files.readString(DISCARD_INNER_CLASS_EXPECTED));
		String actualLines = toNewLineEndings(Files.readString(resultFile));

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testEnums() throws Exception {
		Path resultFile = Files.createTempFile("enums", ".mapping");

		DropInvalidMappingsCommand.run(ENUMS_JAR, ENUMS_INPUT, resultFile);

		String expectedLines = toNewLineEndings(Files.readString(ENUMS_EXPECTED));
		String actualLines = toNewLineEndings(Files.readString(resultFile));

		Assertions.assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testExtraParams() throws Exception {
		Path resultFile = Files.createTempFile("extraTrailingParams", ".mappings");

		DropInvalidMappingsCommand.run(DROP_INVALID_MAPPINGS_JAR, PARAM_INDEXES_INPUT, resultFile);
		String expectedLines = toNewLineEndings(Files.readString(PARAM_INDEXES_EXPECTED));
		String actualLines = toNewLineEndings(Files.readString(resultFile));

		Assertions.assertEquals(expectedLines, actualLines);
	}
}

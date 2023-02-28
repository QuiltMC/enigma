package cuchaz.enigma.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class CheckMappingsCommandTest extends CommandTest {
	private static final Path JAR = obfJar("packageAccess");
	private static final Path WRONG_MAPPINGS = getResource("/packageAccess/wrongMappings");
	private static final Path CORRECT_MAPPINGS = getResource("/packageAccess/correctMappings");

	@Test
	public void testWrong() {
		Assertions.assertThrows(IllegalStateException.class, () ->
				CheckMappingsCommand.run(JAR, WRONG_MAPPINGS)
		);
	}

	@Test
	public void testRight() {
		Assertions.assertDoesNotThrow(() -> CheckMappingsCommand.run(JAR, CORRECT_MAPPINGS));
	}
}

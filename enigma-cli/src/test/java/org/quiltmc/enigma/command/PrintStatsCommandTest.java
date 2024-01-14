package org.quiltmc.enigma.command;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma.TestUtil.getResource;

public class PrintStatsCommandTest {
	private static final Path JAR = TestUtil.obfJar("lone_class");
	private static final Path MAPPINGS = getResource("/print_stats");

	@Test
	public void test() throws Exception {
		// just here to manually verify output
		PrintStatsCommand.run(JAR, MAPPINGS, null, null);
	}
}

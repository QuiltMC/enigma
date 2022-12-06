package cuchaz.enigma.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class CheckMappingsCommandTest {
	private static final String PACKAGE_ACCESS = "../enigma/build/test-obf/packageAccess.jar";

	@Test
	public void testWrong() {
		Assertions.assertThrows(IllegalStateException.class, () ->
				new CheckMappingsCommand().run(new File(PACKAGE_ACCESS).getAbsolutePath(), new File("src/test/resources" +
						"/packageAccess/wrongMappings").getAbsolutePath())
		);
	}

	@Test
	public void testRight() throws Exception {
		new CheckMappingsCommand().run(new File(PACKAGE_ACCESS).getAbsolutePath(), new File("src/test/resources" +
				"/packageAccess/correctMappings").getAbsolutePath());
	}
}

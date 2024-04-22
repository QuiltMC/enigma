package org.quiltmc.enigma;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class MappingFormatDetectionTest {
	@Test
	void testFormatDetection() {
		File formatsDir = new File(TestUtil.getResource("/formats").toUri());

		for (File file : Objects.requireNonNull(formatsDir.listFiles())) {
			Optional<FileType> parsedFormat = Enigma.create().parseFileType(file.toPath());
			// todo

			//Assertions.assertSame(expectedFormat, parsedFormat, "Failed to detect format for " + file.getName());
		}
	}
}

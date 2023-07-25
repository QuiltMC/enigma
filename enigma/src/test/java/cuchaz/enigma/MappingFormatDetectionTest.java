package cuchaz.enigma;

import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

public class MappingFormatDetectionTest {
	@Test
	void testFormatDetection() {
		File formatsDir = new File(TestUtil.getResource("/formats").toUri());

		for (File file : Objects.requireNonNull(formatsDir.listFiles())) {
			MappingFormat parsedFormat = MappingFormat.parseFromFile(file.toPath());
			MappingFormat expectedFormat = MappingFormat.valueOf(file.getName().toUpperCase().split("_EXAMPLE_MAPPING")[0]);

			Assertions.assertSame(expectedFormat, parsedFormat, "Failed to detect format for " + file.getName());
		}
	}
}

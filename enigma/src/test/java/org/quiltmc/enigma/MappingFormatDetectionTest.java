package org.quiltmc.enigma;

import com.google.common.io.MoreFiles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class MappingFormatDetectionTest {
	@Test
	void testFormatDetection() {
		File formatsDir = new File(TestUtil.getResource("/formats").toUri());
		Enigma enigma = Enigma.create();

		for (File file : Objects.requireNonNull(formatsDir.listFiles())) {
			Optional<ReadWriteService> parsedFormat = enigma.getReadWriteService(file.toPath());
			if (parsedFormat.isEmpty()) {
				Assertions.fail("Failed to detect format for " + file.getName());
			}

			String expectedId;
			if (file.isDirectory()) {
				Assertions.assertInstanceOf(FileType.Directory.class, parsedFormat.get().getFileType(), "Failed to detect directory format for " + file.getName());

				expectedId = switch (file.getName()) {
					case "enigma_directory_example_mapping" -> "enigma:enigma_directory";
					default -> throw new IllegalStateException("Unexpected read/write service ID: " + parsedFormat.get().getId());
				};
			} else {
				expectedId = switch (MoreFiles.getFileExtension(file.toPath())) {
					case "tiny" -> "enigma:tiny_v2";
					case "tsrg" -> "enigma:srg_file";
					case "txt" -> "enigma:proguard";
					case "zip" -> "enigma:enigma_zip";
					case "mapping" -> "enigma:enigma_file";
					default -> throw new IllegalStateException("Unexpected read/write service ID: " + parsedFormat.get().getId());
				};
			}

			Assertions.assertEquals(expectedId, parsedFormat.get().getId());
		}
	}
}

package org.quiltmc.enigma;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMainJarMapped {
	private static final Path JAR = TestUtil.obfJar("main_jar_mapped");

	public static final String TEST_CLASS_NAME = "a";

	private static EnigmaProject openProject() {
		try {
			return Enigma.create().openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests that when a parameter of a main jar method that's inherited from a lib class is mapped,
	 * the main jar's parameter is mapped; no lib mapping should be created.
	 */
	@Test
	void test() throws IOException {
		final EnigmaProject project = openProject();

		final LocalVariableEntry equalsParam = TestEntryFactory.newParameter(
				TestEntryFactory.newMethod(TEST_CLASS_NAME, "equals", "(Ljava/lang/Object;)Z"),
				1
		);

		project.getRemapper().putMapping(new ValidationContext(null), equalsParam, new EntryMapping("object"));

		final Path mappingsDir = Files.createTempDirectory("main_jar_mapped");
		project.getEnigma()
				.getReadWriteService(mappingsDir)
				.orElseThrow()
				.write(
					project.getRemapper().getMappings(),
					mappingsDir,
					new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, null, null)
				);

		final List<Path> savedFiles = new LinkedList<>();
		try (Stream<Path> paths = Files.walk(mappingsDir)) {
			paths.filter(Files::isRegularFile).forEach(savedFiles::add);
		}

		assertEquals(1, savedFiles.size());

		assertEquals(mappingsDir.resolve(TEST_CLASS_NAME + ".mapping"), savedFiles.get(0));
	}
}

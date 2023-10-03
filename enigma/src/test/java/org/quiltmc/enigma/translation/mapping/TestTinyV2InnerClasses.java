package org.quiltmc.enigma.translation.mapping;

import org.quiltmc.enigma.Enigma;
import org.quiltmc.enigma.EnigmaProject;
import org.quiltmc.enigma.ProgressListener;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.classprovider.ClasspathClassProvider;
import org.quiltmc.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public final class TestTinyV2InnerClasses {
	public static final Path JAR = TestUtil.obfJar("innerClasses");
	public static final Path MAPPINGS = TestUtil.getResource("/tinyV2InnerClasses/");

	@Test
	public void testMappings() throws Exception {
		EnigmaProject project = Enigma.create().openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		project.setMappings(EnigmaMappingsReader.DIRECTORY.read(MAPPINGS, ProgressListener.none()));
	}
}

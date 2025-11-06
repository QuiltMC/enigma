package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;

import java.nio.file.Path;

public final class TestTinyV2InnerClasses {
	public static final Path JAR = TestUtil.obfJar("inner_classes");
	public static final Path MAPPINGS = TestUtil.getResource("/tinyV2InnerClasses/");

	@Test
	public void testMappings() throws Exception {
		EnigmaProject project = Enigma.create().openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		project.setMappings(EnigmaMappingsReader.DIRECTORY.read(MAPPINGS, ProgressListener.createEmpty()), ProgressListener.createEmpty());
	}
}

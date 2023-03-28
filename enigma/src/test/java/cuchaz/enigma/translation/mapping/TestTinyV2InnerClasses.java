package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.TestUtil;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public final class TestTinyV2InnerClasses {
	public static final Path JAR = TestUtil.obfJar("innerClasses");
	public static final Path MAPPINGS = TestUtil.getResource("/tinyV2InnerClasses/");

	@Test
	public void testMappings() throws Exception {
		EnigmaProject project = Enigma.create().openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		project.setMappings(EnigmaMappingsReader.DIRECTORY.read(MAPPINGS, ProgressListener.none(), project.getEnigma().getProfile().getMappingSaveParameters()));
	}
}

package cuchaz.enigma;

import java.nio.file.Path;

public final class TestUtil {
	public static final Path OBF_JARS_DIR = Path.of("build/test-obf");

	private TestUtil() {
	}

	public static Path obfJar(String name) {
		return OBF_JARS_DIR.resolve(name + ".jar");
	}
}

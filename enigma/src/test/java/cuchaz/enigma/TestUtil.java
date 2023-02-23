package cuchaz.enigma;

import java.net.URISyntaxException;
import java.nio.file.Path;

public final class TestUtil {
	public static final Path OBF_JARS_DIR = Path.of("build/test-obf");
	public static final Path DEOBF_JARS_DIR = Path.of("build/test-deobf");

	private TestUtil() {
	}

	public static Path obfJar(String name) {
		return OBF_JARS_DIR.resolve(name + ".jar");
	}

	public static Path deobfJar(String name) {
		return DEOBF_JARS_DIR.resolve(name + ".jar");
	}

	public static Path getResource(String name) {
		try {
			return Path.of(TestUtil.class.getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}

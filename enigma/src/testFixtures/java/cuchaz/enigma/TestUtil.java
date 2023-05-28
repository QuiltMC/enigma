package cuchaz.enigma;

import java.net.URISyntaxException;
import java.nio.file.Path;

public final class TestUtil {
	private TestUtil() {
	}

	public static Path obfJar(String name) {
		return Path.of("../enigma/build/test-obf/%s.jar".formatted(name)).toAbsolutePath();
	}

	public static Path deobfJar(String name) {
		return Path.of("../enigma/build/test-deobf/%s.jar".formatted(name)).toAbsolutePath();
	}

	public static Path getResource(String name) {
		try {
			return Path.of(TestUtil.class.getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}

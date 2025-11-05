package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestConsistentVersions {
	private static final String MAJOR = "major";
	private static final String MINOR = "minor";
	private static final String PATCH = "patch";

	private static final Pattern VERSION = Pattern.compile(
			"^(?<" + MAJOR + ">\\d+)\\.(?<" + MINOR + ">\\d+)\\.(?<" + PATCH + ">\\d+)(\\+.*)?$"
	);

	@Test
	void test() {
		final Matcher matcher = VERSION.matcher(Enigma.VERSION);
		if (matcher.matches()) {
			Assertions.assertEquals(matcher.group(MAJOR), Integer.toString(Enigma.MAJOR_VERSION));
			Assertions.assertEquals(matcher.group(MINOR), Integer.toString(Enigma.MINOR_VERSION));
			Assertions.assertEquals(matcher.group(PATCH), Integer.toString(Enigma.PATCH_VERSION));
		} else {
			throw new IllegalStateException("Failed to parse Enigma.VERSION!");
		}
	}
}

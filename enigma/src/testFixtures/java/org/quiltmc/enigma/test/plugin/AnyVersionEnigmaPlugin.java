package org.quiltmc.enigma.test.plugin;

import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.util.Version;

public interface AnyVersionEnigmaPlugin extends EnigmaPlugin {
	@SuppressWarnings("NullableProblems")
	@Override
	default boolean supportsEnigmaVersion(Version enigmaVersion) {
		return true;
	}
}

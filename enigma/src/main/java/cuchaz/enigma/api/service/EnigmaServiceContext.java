package cuchaz.enigma.api.service;

import java.nio.file.Path;
import java.util.Optional;

public interface EnigmaServiceContext<T extends EnigmaService> {
	static <T extends EnigmaService> EnigmaServiceContext<T> empty() {
		return key -> Optional.empty();
	}

	Optional<String> getArgument(String key);

	default Path getPath(String path) {
		return Path.of(path);
	}
}

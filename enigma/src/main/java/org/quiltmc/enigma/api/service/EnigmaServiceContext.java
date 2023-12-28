package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.util.Either;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface EnigmaServiceContext<T extends EnigmaService> {
	static <T extends EnigmaService> EnigmaServiceContext<T> empty() {
		return key -> Optional.empty();
	}

	Optional<Either<String, List<String>>> getArgument(String key);

	default Optional<String> getSingleArgument(String key) {
		return this.getArgument(key).flatMap(e -> e.left());
	}

	default Optional<List<String>> getMultipleArguments(String key) {
		return this.getArgument(key).flatMap(e -> e.right());
	}

	default Path getPath(String path) {
		return Path.of(path);
	}
}

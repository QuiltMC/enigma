package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.net.URISyntaxException;
import java.nio.file.Path;

public abstract class CommandTest {
	public static Path obfJar(String name) {
		return Path.of("../enigma/build/test-obf/%s.jar".formatted(name)).toAbsolutePath();
	}

	public static Path getResource(String name) {
		try {
			return Path.of(CommandTest.class.getResource(name).toURI()).toAbsolutePath();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	protected static String getName(EntryTree<EntryMapping> mappings, Entry<?> entry) {
		if (!mappings.contains(entry)) {
			return null;
		}

		EntryMapping mapping = mappings.get(entry);
		return mapping != null ? mapping.targetName() : null;
	}
}

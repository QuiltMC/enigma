package org.quiltmc.enigma.translation.mapping;

import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class TestComments {
	private static final Path DIRECTORY = TestUtil.getResource("/comments/");

	@Test
	public void testParseAndWrite() throws IOException, MappingParseException {
		MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(
						DIRECTORY);

		new TinyV2Writer("intermediary", "named")
						.write(mappings, DIRECTORY.resolve("convertedtiny.tiny"), params);
	}
}

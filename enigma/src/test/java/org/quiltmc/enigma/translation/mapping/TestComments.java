package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import java.io.IOException;
import java.nio.file.Path;

public class TestComments {
	private static final Path DIRECTORY = TestUtil.getResource("/comments/");

	@Test
	public void testParseAndWrite() throws IOException, MappingParseException {
		MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, "intermediary", "named");
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(
						DIRECTORY);

		Path file = DIRECTORY.resolve("convertedtiny.tiny");

		Enigma.create().getReadWriteService(file).get().write(mappings, MappingDelta.added(mappings), file, ProgressListener.createEmpty(), params);
	}
}

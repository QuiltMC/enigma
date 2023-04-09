package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.TestUtil;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class TestComments {
	private static final Path DIRECTORY = TestUtil.getResource("/comments/");

	@Test
	public void testParseAndWrite() throws IOException, MappingParseException {
		ProgressListener progressListener = ProgressListener.none();
		MappingSaveParameters params = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> mappings = EnigmaMappingsReader.DIRECTORY.read(
						DIRECTORY, progressListener, params);

		new TinyV2Writer("intermediary", "named")
						.write(mappings, DIRECTORY.resolve("convertedtiny.tiny"), progressListener, params);
	}
}

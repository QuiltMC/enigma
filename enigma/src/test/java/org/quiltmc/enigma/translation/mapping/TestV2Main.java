package org.quiltmc.enigma.translation.mapping;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestV2Main {
	public static void main(String... args) throws Exception {
		Path path = TestTinyV2InnerClasses.MAPPINGS;

		MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, "obf", "deobf");

		EntryTree<EntryMapping> tree = EnigmaMappingsReader.DIRECTORY.read(path);
		Path file = Paths.get("currentYarn.tiny");

		Enigma.create().getReadWriteService(file).get().write(tree, MappingDelta.added(tree), file, ProgressListener.createEmpty(), parameters);
	}
}

package cuchaz.enigma.translation.mapping.serde.recaf;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecaf {

    @Test
    public void testIntegrity() throws Exception {
        Path path = Path.of("src/test/resources/recaf.mappings");
        Set<String> contents = new HashSet<>(Files.readAllLines(path));
        Path tempFile = Files.createTempFile(null, null);
        Files.write(tempFile, contents);

        RecafMappingsWriter writer = RecafMappingsWriter.INSTANCE;
        RecafMappingsReader reader = RecafMappingsReader.INSTANCE;

        EntryTree<EntryMapping> mappings = reader.read(tempFile, ProgressListener.none(), null);
        writer.write(mappings, tempFile, ProgressListener.none(), null);

        reader.read(tempFile, ProgressListener.none(), null);
        Set<String> newContents = new HashSet<>(Files.readAllLines(tempFile));

        assertEquals(contents, newContents);
    }
}

package cuchaz.enigma.translation.mapping.serde.recaf;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecaf {

    @Test
    public void testIntegrity() throws Exception {
        List<String> contents;
        try (InputStream in = getClass().getResourceAsStream("/recaf.mappings")) {
            contents = Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R"));
        }

        Path path = Path.of(getClass().getResource("recaf.mappings").getPath());
        Files.writeString(path, String.join("\n", contents));

        RecafMappingsWriter writer = RecafMappingsWriter.INSTANCE;
        RecafMappingsReader reader = RecafMappingsReader.INSTANCE;

        EntryTree<EntryMapping> mappings = reader.read(path, ProgressListener.none(), null);
        writer.write(mappings, path, ProgressListener.none(), null);

        reader.read(path, ProgressListener.none(), null);
        List<String> newContents = Files.readAllLines(path);

        assertEquals(contents, newContents);
    }
}

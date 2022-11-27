package cuchaz.enigma.translation.mapping.serde.recaf;

import com.google.common.jimfs.Jimfs;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecaf {

    @Test
    public void testIntegrity() throws Exception {
        Set<String> contents;
        try (InputStream in = getClass().getResourceAsStream("/recaf.mappings")) {
            contents = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().collect(Collectors.toSet());
        }

        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("recaf.mappings");
            Files.write(path, contents);

            RecafMappingsWriter writer = RecafMappingsWriter.INSTANCE;
            RecafMappingsReader reader = RecafMappingsReader.INSTANCE;

            EntryTree<EntryMapping> mappings = reader.read(path, ProgressListener.none(), null);
            writer.write(mappings, path, ProgressListener.none(), null);

            reader.read(path, ProgressListener.none(), null);
            Set<String> newContents = new HashSet<>(Files.readAllLines(path));

            assertEquals(contents, newContents);
        }
    }
}

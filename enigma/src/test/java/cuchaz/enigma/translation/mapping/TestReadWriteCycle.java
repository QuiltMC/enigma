package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * Tests that a MappingFormat can write out a fixed set of mappings and read them back without losing any information.
 * Javadoc skipped for Tiny (v1) as it doesn't support them.
 */
public class TestReadWriteCycle {

	private final MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

	private final Pair<ClassEntry, EntryMapping> testClazz = new Pair<>(
			new ClassEntry("a/b/c"),
			new EntryMapping("alpha/beta/charlie", "this is a test class")
	);

	private final Pair<FieldEntry, EntryMapping> testField1 = new Pair<>(
			FieldEntry.parse("a/b/c", "field1", "I"),
			new EntryMapping("mapped1", "this is field 1")
	);

	private final Pair<FieldEntry, EntryMapping> testField2 = new Pair<>(
			FieldEntry.parse("a/b/c", "field2", "I"),
			new EntryMapping("mapped2", "this is field 2")
	);

	private final Pair<MethodEntry, EntryMapping> testMethod1 = new Pair<>(
			MethodEntry.parse("a/b/c", "method1", "()V"),
			new EntryMapping("mapped3", "this is method1")
	);

	private final Pair<MethodEntry, EntryMapping> testMethod2 = new Pair<>(
			MethodEntry.parse("a/b/c", "method2", "()V"),
			new EntryMapping("mapped4", "this is method 2")
	);

	private void insertMapping(EntryTree<EntryMapping> mappings, Pair<? extends Entry<?>, EntryMapping> mappingPair){
		mappings.insert(mappingPair.a, mappingPair.b);
	}

	private void testReadWriteCycle(MappingFormat mappingFormat, boolean supportsJavadoc, String tmpNameSuffix) throws IOException, MappingParseException {
		//construct some known mappings to test with
		EntryTree<EntryMapping> testMappings = new HashEntryTree<>();
		insertMapping(testMappings, testClazz);
		insertMapping(testMappings, testField1);
		insertMapping(testMappings, testField2);
		insertMapping(testMappings, testMethod1);
		insertMapping(testMappings, testMethod2);

		Assertions.assertTrue(testMappings.contains(testClazz.a), "Test mapping insertion failed: testClazz");
		Assertions.assertTrue(testMappings.contains(testField1.a), "Test mapping insertion failed: testField1");
		Assertions.assertTrue(testMappings.contains(testField2.a), "Test mapping insertion failed: testField2");
		Assertions.assertTrue(testMappings.contains(testMethod1.a), "Test mapping insertion failed: testMethod1");
		Assertions.assertTrue(testMappings.contains(testMethod2.a), "Test mapping insertion failed: testMethod2");

		File tempFile = File.createTempFile("readWriteCycle", tmpNameSuffix);
		tempFile.delete();//remove the auto created file


		mappingFormat.write(testMappings, tempFile.toPath(), ProgressListener.none(), parameters);
		Assertions.assertTrue(tempFile.exists(), "Written file not created");

		EntryTree<EntryMapping> loadedMappings = mappingFormat.read(tempFile.toPath(), ProgressListener.none(), parameters);

		Assertions.assertTrue(loadedMappings.contains(testClazz.a), "Loaded mappings don't contain testClazz");
		Assertions.assertTrue(loadedMappings.contains(testField1.a), "Loaded mappings don't contain testField1");
		Assertions.assertTrue(loadedMappings.contains(testField2.a), "Loaded mappings don't contain testField2");
		Assertions.assertTrue(loadedMappings.contains(testMethod1.a), "Loaded mappings don't contain testMethod1");
		Assertions.assertTrue(loadedMappings.contains(testMethod2.a), "Loaded mappings don't contain testMethod2");

		Assertions.assertEquals(testClazz.b.targetName(), loadedMappings.get(testClazz.a).targetName(), "Incorrect mapping: testClazz");
		Assertions.assertEquals(testField1.b.targetName(), loadedMappings.get(testField1.a).targetName(), "Incorrect mapping: testField1");
		Assertions.assertEquals(testField2.b.targetName(), loadedMappings.get(testField2.a).targetName(), "Incorrect mapping: testField2");
		Assertions.assertEquals(testMethod1.b.targetName(), loadedMappings.get(testMethod1.a).targetName(), "Incorrect mapping: testMethod1");
		Assertions.assertEquals(testMethod2.b.targetName(), loadedMappings.get(testMethod2.a).targetName(), "Incorrect mapping: testMethod2");

		if (supportsJavadoc) {
			Assertions.assertEquals(testClazz.b.javadoc(), loadedMappings.get(testClazz.a).javadoc(), "Incorrect javadoc: testClazz");
			Assertions.assertEquals(testField1.b.javadoc(), loadedMappings.get(testField1.a).javadoc(), "Incorrect javadoc: testField1");
			Assertions.assertEquals(testField2.b.javadoc(), loadedMappings.get(testField2.a).javadoc(), "Incorrect javadoc: testField2");
			Assertions.assertEquals(testMethod1.b.javadoc(), loadedMappings.get(testMethod1.a).javadoc(), "Incorrect javadoc: testMethod1");
			Assertions.assertEquals(testMethod2.b.javadoc(), loadedMappings.get(testMethod2.a).javadoc(), "Incorrect javadoc: testMethod2");
		}

		tempFile.delete();
	}

	@Test
	public void testEnigmaFile() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.ENIGMA_FILE, true, ".enigma");
	}

	@Test
	public void testEnigmaDir() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.ENIGMA_DIRECTORY, true, ".tmp");
	}

	@Test
	public void testEnigmaZip() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.ENIGMA_ZIP, true, ".zip");
	}

	@Test
	public void testTinyFile() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.TINY_FILE, false, ".tiny");
	}

	@Test
	public void testTinyV2() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.TINY_V2, true, ".tinyv2");
	}
}

package org.quiltmc.enigma.translation.mapping;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * Tests that a MappingFormat can write out a fixed set of mappings and read them back without losing any information.
 */
public class TestReadWriteCycle {
	private final MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false);

	private final Pair<ClassEntry, EntryMapping> testClazz = new Pair<>(
			new ClassEntry("a/b/c"),
			new EntryMapping("alpha/beta/charlie", "this is a test class", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<FieldEntry, EntryMapping> testField1 = new Pair<>(
			FieldEntry.parse("a/b/c", "field1", "I"),
			new EntryMapping("mapped1", "this is field 1", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<FieldEntry, EntryMapping> testField2 = new Pair<>(
			FieldEntry.parse("a/b/c", "field2", "I"),
			new EntryMapping("mapped2", "this is field 2", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<MethodEntry, EntryMapping> testMethod1 = new Pair<>(
			MethodEntry.parse("a/b/c", "method1", "()V"),
			new EntryMapping("mapped3", "this is method1", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<MethodEntry, EntryMapping> testMethod2 = new Pair<>(
			MethodEntry.parse("a/b/c", "method2", "()V"),
			new EntryMapping("mapped4", "this is method 2", TokenType.DEOBFUSCATED, null)
	);

	private void insertMapping(EntryTree<EntryMapping> mappings, Pair<? extends Entry<?>, EntryMapping> mappingPair) {
		mappings.insert(mappingPair.a(), mappingPair.b());
	}

	private void testReadWriteCycle(ReadWriteService readWriteService, String tmpNameSuffix) throws IOException, MappingParseException {
		//construct some known mappings to test with
		EntryTree<EntryMapping> testMappings = new HashEntryTree<>();
		this.insertMapping(testMappings, this.testClazz);
		this.insertMapping(testMappings, this.testField1);
		this.insertMapping(testMappings, this.testField2);
		this.insertMapping(testMappings, this.testMethod1);
		this.insertMapping(testMappings, this.testMethod2);

		Assertions.assertTrue(testMappings.contains(this.testClazz.a()), "Test mapping insertion failed: testClazz");
		Assertions.assertTrue(testMappings.contains(this.testField1.a()), "Test mapping insertion failed: testField1");
		Assertions.assertTrue(testMappings.contains(this.testField2.a()), "Test mapping insertion failed: testField2");
		Assertions.assertTrue(testMappings.contains(this.testMethod1.a()), "Test mapping insertion failed: testMethod1");
		Assertions.assertTrue(testMappings.contains(this.testMethod2.a()), "Test mapping insertion failed: testMethod2");

		File tempFile = File.createTempFile("readWriteCycle", tmpNameSuffix);
		tempFile.delete(); //remove the auto created file

		readWriteService.write(testMappings, tempFile.toPath(), ProgressListener.createEmpty(), this.parameters);
		Assertions.assertTrue(tempFile.exists(), "Written file not created");

		EntryTree<EntryMapping> loadedMappings = readWriteService.read(tempFile.toPath(), ProgressListener.createEmpty());

		Assertions.assertTrue(loadedMappings.contains(this.testClazz.a()), "Loaded mappings don't contain testClazz");
		Assertions.assertTrue(loadedMappings.contains(this.testField1.a()), "Loaded mappings don't contain testField1");
		Assertions.assertTrue(loadedMappings.contains(this.testField2.a()), "Loaded mappings don't contain testField2");
		Assertions.assertTrue(loadedMappings.contains(this.testMethod1.a()), "Loaded mappings don't contain testMethod1");
		Assertions.assertTrue(loadedMappings.contains(this.testMethod2.a()), "Loaded mappings don't contain testMethod2");

		Assertions.assertEquals(this.testClazz.b().targetName(), loadedMappings.get(this.testClazz.a()).targetName(), "Incorrect mapping: testClazz");
		Assertions.assertEquals(this.testField1.b().targetName(), loadedMappings.get(this.testField1.a()).targetName(), "Incorrect mapping: testField1");
		Assertions.assertEquals(this.testField2.b().targetName(), loadedMappings.get(this.testField2.a()).targetName(), "Incorrect mapping: testField2");
		Assertions.assertEquals(this.testMethod1.b().targetName(), loadedMappings.get(this.testMethod1.a()).targetName(), "Incorrect mapping: testMethod1");
		Assertions.assertEquals(this.testMethod2.b().targetName(), loadedMappings.get(this.testMethod2.a()).targetName(), "Incorrect mapping: testMethod2");

		Assertions.assertEquals(this.testClazz.b().javadoc(), loadedMappings.get(this.testClazz.a()).javadoc(), "Incorrect javadoc: testClazz");
		Assertions.assertEquals(this.testField1.b().javadoc(), loadedMappings.get(this.testField1.a()).javadoc(), "Incorrect javadoc: testField1");
		Assertions.assertEquals(this.testField2.b().javadoc(), loadedMappings.get(this.testField2.a()).javadoc(), "Incorrect javadoc: testField2");
		Assertions.assertEquals(this.testMethod1.b().javadoc(), loadedMappings.get(this.testMethod1.a()).javadoc(), "Incorrect javadoc: testMethod1");
		Assertions.assertEquals(this.testMethod2.b().javadoc(), loadedMappings.get(this.testMethod2.a()).javadoc(), "Incorrect javadoc: testMethod2");

		tempFile.delete();
	}

	@Test
	public void testEnigmaFile() throws IOException, MappingParseException {
		this.testReadWriteCycle(MappingFormat.ENIGMA_FILE, ".enigma");
	}

	@Test
	public void testEnigmaDir() throws IOException, MappingParseException {
		this.testReadWriteCycle(MappingFormat.ENIGMA_DIRECTORY, ".tmp");
	}

	@Test
	public void testEnigmaZip() throws IOException, MappingParseException {
		this.testReadWriteCycle(MappingFormat.ENIGMA_ZIP, ".zip");
	}

	@Test
	public void testTinyV2() throws IOException, MappingParseException {
		this.testReadWriteCycle(MappingFormat.TINY_V2, ".tiny");
	}
}

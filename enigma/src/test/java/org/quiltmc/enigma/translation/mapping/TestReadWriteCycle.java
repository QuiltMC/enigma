package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * Tests that a MappingFormat can write out a fixed set of mappings and read them back without losing any information.
 */
public class TestReadWriteCycle {
	private final MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, null, null);
	private final Enigma enigma = Enigma.create();

	private final Pair<ClassEntry, EntryMapping> testClazz = new Pair<>(
			new ClassEntry("a/b/c"),
			new EntryMapping("alpha/beta/charlie", "this is a test class", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<ClassEntry, EntryMapping> testProposedClazz = new Pair<>(
			new ClassEntry("a/b/c/d"),
			new EntryMapping("alpha/beta/charlie/delta", "this is a test class!", TokenType.JAR_PROPOSED, "enigma:fake_plugin")
	);

	private final Pair<FieldEntry, EntryMapping> testField1 = new Pair<>(
			FieldEntry.parse("a/b/c", "field1", "I"),
			new EntryMapping("mapped1", "this is field 1", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<FieldEntry, EntryMapping> testField2 = new Pair<>(
			FieldEntry.parse("a/b/c", "field2", "I"),
			new EntryMapping("mapped2", "this is field 2", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<FieldEntry, EntryMapping> testProposedField = new Pair<>(
			FieldEntry.parse("a/b/c", "proposedField", "I"),
			new EntryMapping("proposed1", "this is a proposed field", TokenType.JAR_PROPOSED, "enigma:fake_plugin")
	);

	private final Pair<MethodEntry, EntryMapping> testMethod1 = new Pair<>(
			MethodEntry.parse("a/b/c", "method1", "()V"),
			new EntryMapping("mapped3", "this is method1", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<MethodEntry, EntryMapping> testMethod2 = new Pair<>(
			MethodEntry.parse("a/b/c", "method2", "()V"),
			new EntryMapping("mapped4", "this is method 2", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<MethodEntry, EntryMapping> testMethod3 = new Pair<>(
			MethodEntry.parse("a/b/c", "method3", "(IZ)V"),
			new EntryMapping("mapped5", "this is method 3", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<MethodEntry, EntryMapping> testProposedMethod = new Pair<>(
			MethodEntry.parse("a/b/c", "proposedMethod", "()V"),
			new EntryMapping("proposed2", "this is a proposed method", TokenType.JAR_PROPOSED, "enigma:fake_plugin")
	);

	private final Pair<LocalVariableEntry, EntryMapping> testNormalParameter = new Pair<>(
			TestEntryFactory.newParameter(this.testMethod3.a(), 0),
			new EntryMapping("mapped6", "this is a normal parameter", TokenType.DEOBFUSCATED, null)
	);

	private final Pair<LocalVariableEntry, EntryMapping> testProposedParameter = new Pair<>(
			TestEntryFactory.newParameter(this.testMethod3.a(), 1),
			new EntryMapping("mapped7", "this is a proposed parameter", TokenType.JAR_PROPOSED, "enigma:fake_plugin")
	);

	private void insertMapping(EntryTree<EntryMapping> mappings, Pair<? extends Entry<?>, EntryMapping> mappingPair) {
		mappings.insert(mappingPair.a(), mappingPair.b());
	}

	private void testReadWriteCycle(ReadWriteService readWriteService, String tmpNameSuffix) throws IOException, MappingParseException {
		//construct some known mappings to test with
		EntryTree<EntryMapping> testMappings = new HashEntryTree<>();
		this.insertMapping(testMappings, this.testClazz);
		this.insertMapping(testMappings, this.testProposedClazz);
		this.insertMapping(testMappings, this.testField1);
		this.insertMapping(testMappings, this.testField2);
		this.insertMapping(testMappings, this.testProposedField);
		this.insertMapping(testMappings, this.testMethod1);
		this.insertMapping(testMappings, this.testMethod2);
		this.insertMapping(testMappings, this.testMethod3);
		this.insertMapping(testMappings, this.testProposedMethod);
		this.insertMapping(testMappings, this.testNormalParameter);
		this.insertMapping(testMappings, this.testProposedParameter);

		Assertions.assertTrue(testMappings.contains(this.testClazz.a()), "Test mapping insertion failed: testClazz");
		Assertions.assertTrue(testMappings.contains(this.testProposedClazz.a()), "Test mapping insertion failed: testProposedClazz");
		Assertions.assertTrue(testMappings.contains(this.testField1.a()), "Test mapping insertion failed: testField1");
		Assertions.assertTrue(testMappings.contains(this.testField2.a()), "Test mapping insertion failed: testField2");
		Assertions.assertTrue(testMappings.contains(this.testProposedField.a()), "Test mapping insertion failed: testProposedField");
		Assertions.assertTrue(testMappings.contains(this.testMethod1.a()), "Test mapping insertion failed: testMethod1");
		Assertions.assertTrue(testMappings.contains(this.testMethod2.a()), "Test mapping insertion failed: testMethod2");
		Assertions.assertTrue(testMappings.contains(this.testMethod3.a()), "Test mapping insertion failed: testMethod3");
		Assertions.assertTrue(testMappings.contains(this.testProposedMethod.a()), "Test mapping insertion failed: testProposedMethod");
		Assertions.assertTrue(testMappings.contains(this.testNormalParameter.a()), "Test mapping insertion failed: testNormalParameter");
		Assertions.assertTrue(testMappings.contains(this.testProposedParameter.a()), "Test mapping insertion failed: testProposedParameter");

		File tempFile = File.createTempFile("readWriteCycle", tmpNameSuffix);
		tempFile.delete(); //remove the auto created file

		readWriteService.write(testMappings, tempFile.toPath(), ProgressListener.createEmpty(), this.parameters);
		Assertions.assertTrue(tempFile.exists(), "Written file not created");

		EntryTree<EntryMapping> loadedMappings = readWriteService.read(tempFile.toPath(), ProgressListener.createEmpty());

		Assertions.assertTrue(loadedMappings.contains(this.testClazz.a()), "Loaded mappings don't contain testClazz");
		Assertions.assertTrue(loadedMappings.contains(this.testProposedClazz.a()), "Loaded mappings don't contain testProposedClazz");
		Assertions.assertTrue(loadedMappings.contains(this.testField1.a()), "Loaded mappings don't contain testField1");
		Assertions.assertTrue(loadedMappings.contains(this.testField2.a()), "Loaded mappings don't contain testField2");
		Assertions.assertTrue(loadedMappings.contains(this.testProposedField.a()), "Loaded mappings don't contain testProposedField");
		Assertions.assertTrue(loadedMappings.contains(this.testMethod1.a()), "Loaded mappings don't contain testMethod1");
		Assertions.assertTrue(loadedMappings.contains(this.testMethod2.a()), "Loaded mappings don't contain testMethod2");
		Assertions.assertTrue(loadedMappings.contains(this.testMethod3.a()), "Loaded mappings don't contain testMethod3");
		Assertions.assertTrue(loadedMappings.contains(this.testProposedMethod.a()), "Loaded mappings don't contain testProposedMethod");
		Assertions.assertTrue(loadedMappings.contains(this.testNormalParameter.a()), "Loaded mappings don't contain testNormalParameter");
		Assertions.assertTrue(loadedMappings.contains(this.testProposedParameter.a()), "Loaded mappings don't contain testProposedParameter");

		Assertions.assertEquals(this.testClazz.b().targetName(), loadedMappings.get(this.testClazz.a()).targetName(), "Incorrect mapping: testClazz");
		// note: proposed class name will not be saved
		Assertions.assertEquals(this.testField1.b().targetName(), loadedMappings.get(this.testField1.a()).targetName(), "Incorrect mapping: testField1");
		Assertions.assertEquals(this.testField2.b().targetName(), loadedMappings.get(this.testField2.a()).targetName(), "Incorrect mapping: testField2");
		// note: proposed field name will not be saved
		Assertions.assertEquals(this.testMethod1.b().targetName(), loadedMappings.get(this.testMethod1.a()).targetName(), "Incorrect mapping: testMethod1");
		Assertions.assertEquals(this.testMethod2.b().targetName(), loadedMappings.get(this.testMethod2.a()).targetName(), "Incorrect mapping: testMethod2");
		Assertions.assertEquals(this.testMethod3.b().targetName(), loadedMappings.get(this.testMethod3.a()).targetName(), "Incorrect mapping: testMethod3");
		// note: proposed method name will not be saved
		Assertions.assertEquals(this.testNormalParameter.b().targetName(), loadedMappings.get(this.testNormalParameter.a()).targetName(), "Incorrect mapping: testNormalParameter");
		// note: proposed parameter name will not be saved

		Assertions.assertEquals(this.testClazz.b().javadoc(), loadedMappings.get(this.testClazz.a()).javadoc(), "Incorrect javadoc: testClazz");
		Assertions.assertEquals(this.testProposedClazz.b().javadoc(), loadedMappings.get(this.testProposedClazz.a()).javadoc(), "Incorrect javadoc: testProposedClazz");
		Assertions.assertEquals(this.testField1.b().javadoc(), loadedMappings.get(this.testField1.a()).javadoc(), "Incorrect javadoc: testField1");
		Assertions.assertEquals(this.testField2.b().javadoc(), loadedMappings.get(this.testField2.a()).javadoc(), "Incorrect javadoc: testField2");
		Assertions.assertEquals(this.testProposedField.b().javadoc(), loadedMappings.get(this.testProposedField.a()).javadoc(), "Incorrect javadoc: testProposedField");
		Assertions.assertEquals(this.testMethod1.b().javadoc(), loadedMappings.get(this.testMethod1.a()).javadoc(), "Incorrect javadoc: testMethod1");
		Assertions.assertEquals(this.testMethod2.b().javadoc(), loadedMappings.get(this.testMethod2.a()).javadoc(), "Incorrect javadoc: testMethod2");
		Assertions.assertEquals(this.testMethod3.b().javadoc(), loadedMappings.get(this.testMethod3.a()).javadoc(), "Incorrect javadoc: testMethod3");
		Assertions.assertEquals(this.testProposedMethod.b().javadoc(), loadedMappings.get(this.testProposedMethod.a()).javadoc(), "Incorrect javadoc: testProposedMethod");
		Assertions.assertEquals(this.testNormalParameter.b().javadoc(), loadedMappings.get(this.testNormalParameter.a()).javadoc(), "Incorrect javadoc: testNormalParameter");
		Assertions.assertEquals(this.testProposedParameter.b().javadoc(), loadedMappings.get(this.testProposedParameter.a()).javadoc(), "Incorrect javadoc: testProposedParameter");

		tempFile.delete();
	}

	@Test
	public void testEnigmaFile() throws IOException, MappingParseException {
		this.testReadWriteCycle(this.getService(file -> file.getExtensions().contains("mapping") && !file.isDirectory()), ".mapping");
	}

	@Test
	public void testEnigmaDir() throws IOException, MappingParseException {
		this.testReadWriteCycle(this.getService(file -> file.getExtensions().contains("mapping") && file.isDirectory()), ".tmp");
	}

	@Test
	public void testEnigmaZip() throws IOException, MappingParseException {
		this.testReadWriteCycle(this.getService(file -> file.getExtensions().contains("zip")), ".zip");
	}

	@Test
	public void testTinyV2() throws IOException, MappingParseException {
		this.testReadWriteCycle(this.getService(file -> file.getExtensions().contains("tiny")), ".tiny");
	}

	@SuppressWarnings("all")
	private ReadWriteService getService(Predicate<FileType> predicate) {
		return this.enigma.getReadWriteService(this.enigma.getSupportedFileTypes().stream().filter(predicate).findFirst().get()).get();
	}
}

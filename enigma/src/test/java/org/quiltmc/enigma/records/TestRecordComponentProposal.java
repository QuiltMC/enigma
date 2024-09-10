package org.quiltmc.enigma.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;

public class TestRecordComponentProposal {
	private static final Path JAR = TestUtil.obfJar("records");
	private static EnigmaProject project;

	@BeforeAll
	static void setupEnigma() throws IOException {
		Reader r = new StringReader("""
				{
					"services": {
						"name_proposal": [
							{
								"id": "enigma:record_component_proposer"
							}
						],
						"jar_indexer": [
							{
								"id": "enigma:record_component_indexer"
							}
						]
					}
				}""");

		EnigmaProfile profile = EnigmaProfile.parse(r);
		Enigma enigma = Enigma.builder().setProfile(profile).build();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
	}

	@Test
	void testSimpleRecordComponentProposal() {
		// basic example
		ClassEntry aClass = TestEntryFactory.newClass("a");
		FieldEntry aField = TestEntryFactory.newField(aClass, "a", "I");
		MethodEntry aGetter = TestEntryFactory.newMethod(aClass, "a", "()I");

		Assertions.assertSame(project.getRemapper().getMapping(aField).tokenType(), TokenType.OBFUSCATED);
		Assertions.assertSame(project.getRemapper().getMapping(aGetter).tokenType(), TokenType.OBFUSCATED);

		project.getRemapper().putMapping(TestUtil.newVC(), aField, new EntryMapping("mapped"));

		var fieldMapping = project.getRemapper().getMapping(aField);
		Assertions.assertEquals(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals("mapped", fieldMapping.targetName());

		var getterMapping = project.getRemapper().getMapping(aGetter);
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, getterMapping.tokenType());
		Assertions.assertEquals("mapped", getterMapping.targetName());
		Assertions.assertEquals("enigma:record_component_proposer", getterMapping.sourcePluginId());
	}

	@Test
	void testMismatchRecordComponentProposal() {
		// name of getter mismatches with name of field
		ClassEntry cClass = TestEntryFactory.newClass("d");
		FieldEntry aField = TestEntryFactory.newField(cClass, "a", "I");
		MethodEntry fakeAGetter = TestEntryFactory.newMethod(cClass, "a", "()I");
		MethodEntry realAGetter = TestEntryFactory.newMethod(cClass, "b", "()I");

		Assertions.assertSame(project.getRemapper().getMapping(aField).tokenType(), TokenType.OBFUSCATED);
		Assertions.assertSame(project.getRemapper().getMapping(fakeAGetter).tokenType(), TokenType.OBFUSCATED);
		Assertions.assertSame(project.getRemapper().getMapping(realAGetter).tokenType(), TokenType.OBFUSCATED);

		project.getRemapper().putMapping(TestUtil.newVC(), aField, new EntryMapping("mapped"));

		var fieldMapping = project.getRemapper().getMapping(aField);
		Assertions.assertEquals(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals("mapped", fieldMapping.targetName());

		// fake getter should NOT be mapped
		var fakeGetterMapping = project.getRemapper().getMapping(fakeAGetter);
		Assertions.assertEquals(TokenType.OBFUSCATED, fakeGetterMapping.tokenType());

		// real getter SHOULD be mapped
		var realGetterMapping = project.getRemapper().getMapping(realAGetter);
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, realGetterMapping.tokenType());
		Assertions.assertEquals("mapped", realGetterMapping.targetName());
		Assertions.assertEquals("enigma:record_component_proposer", realGetterMapping.sourcePluginId());
	}

	@Test
	void testRecordComponentMappingRemoval() {
		ClassEntry aClass = TestEntryFactory.newClass("a");
		FieldEntry aField = TestEntryFactory.newField(aClass, "a", "I");
		MethodEntry aGetter = TestEntryFactory.newMethod(aClass, "a", "()I");

		Assertions.assertSame(project.getRemapper().getMapping(aField).tokenType(), TokenType.OBFUSCATED);
		Assertions.assertSame(project.getRemapper().getMapping(aGetter).tokenType(), TokenType.OBFUSCATED);

		// put name, make sure getter matches
		project.getRemapper().putMapping(TestUtil.newVC(), aField, new EntryMapping("mapped"));

		var fieldMapping = project.getRemapper().getMapping(aField);
		Assertions.assertEquals(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		var getterMapping = project.getRemapper().getMapping(aGetter);
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, getterMapping.tokenType());

		// if field becomes obf getter should become obf
		project.getRemapper().putMapping(TestUtil.newVC(), aField, EntryMapping.OBFUSCATED);

		var newFieldMapping = project.getRemapper().getMapping(aField);
		Assertions.assertEquals(TokenType.OBFUSCATED, newFieldMapping.tokenType());
		var newGetterMapping = project.getRemapper().getMapping(aGetter);
		Assertions.assertEquals(TokenType.OBFUSCATED, newGetterMapping.tokenType());
	}

	@Test
	void testTypedRecordComponentProposal() {
		// verify that proposal works on different types, all other tests use int getters
		ClassEntry eClass = TestEntryFactory.newClass("e");
		FieldEntry aField = TestEntryFactory.newField(eClass, "a", "Ljava/lang/String;");
		MethodEntry aGetter = TestEntryFactory.newMethod(eClass, "a", "()Ljava/lang/String;");

		Assertions.assertSame(project.getRemapper().getMapping(aField).tokenType(), TokenType.OBFUSCATED);
		Assertions.assertSame(project.getRemapper().getMapping(aGetter).tokenType(), TokenType.OBFUSCATED);

		project.getRemapper().putMapping(TestUtil.newVC(), aField, new EntryMapping("mapped"));

		var fieldMapping = project.getRemapper().getMapping(aField);
		Assertions.assertEquals(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals("mapped", fieldMapping.targetName());

		var getterMapping = project.getRemapper().getMapping(aGetter);
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, getterMapping.tokenType());
		Assertions.assertEquals("mapped", getterMapping.targetName());
		Assertions.assertEquals("enigma:record_component_proposer", getterMapping.sourcePluginId());
	}
}

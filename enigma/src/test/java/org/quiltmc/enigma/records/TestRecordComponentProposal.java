package org.quiltmc.enigma.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import org.quiltmc.enigma.impl.plugin.RecordComponentProposalService;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;

/**
 * Many record tests rely on the fact that proguard consistently names things in order a, b, c... which results in
 * most default record component getters having the same name as their fields.<br>
 * Changing proguard's naming configs could break many tests.
 */
public class TestRecordComponentProposal {
	private static final Path JAR = TestUtil.obfJar("records");
	private static EnigmaProject project;

	@BeforeEach
	void setupEnigma() throws IOException {
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

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aGetter).tokenType());

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
	void testFakeGetterWrongInstructions() {
		final ClassEntry fakeGetterWrongInstructionsRecord = TestEntryFactory.newClass("h");
		final FieldEntry componentField = TestEntryFactory.newField(fakeGetterWrongInstructionsRecord, "a", "I");
		final MethodEntry fakeGetter = TestEntryFactory.newMethod(fakeGetterWrongInstructionsRecord, "a", "()I");
		final MethodEntry componentGetter = TestEntryFactory.newMethod(fakeGetterWrongInstructionsRecord, "b", "()I");

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(componentField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(fakeGetter).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(componentGetter).tokenType());

		final String targetName = "mapped";
		project.getRemapper().putMapping(TestUtil.newVC(), componentField, new EntryMapping(targetName));

		final EntryMapping fieldMapping = project.getRemapper().getMapping(componentField);
		Assertions.assertSame(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals(targetName, fieldMapping.targetName());

		// fake getter should NOT be mapped
		final EntryMapping fakeGetterMapping = project.getRemapper().getMapping(fakeGetter);
		Assertions.assertEquals(TokenType.OBFUSCATED, fakeGetterMapping.tokenType());

		// real getter should also NOT be mapped
		// it's impossible to determine that it's the real getter
		// this behavior matches decompilers'
		final EntryMapping componentGetterMapping = project.getRemapper().getMapping(componentGetter);
		Assertions.assertEquals(TokenType.OBFUSCATED, componentGetterMapping.tokenType());
	}

	@Test
	void testFakeGetterRightInstructions() {
		final ClassEntry fakeGetterRightInstructionsRecord = TestEntryFactory.newClass("g");
		final FieldEntry componentField = TestEntryFactory.newField(fakeGetterRightInstructionsRecord, "a", "I");
		final MethodEntry fakeGetter = TestEntryFactory.newMethod(fakeGetterRightInstructionsRecord, "a", "()I");
		final MethodEntry componentGetter = TestEntryFactory.newMethod(fakeGetterRightInstructionsRecord, "b", "()I");

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(componentField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(fakeGetter).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(componentGetter).tokenType());

		final String targetName = "mapped";
		project.getRemapper().putMapping(TestUtil.newVC(), componentField, new EntryMapping(targetName));

		// FAKE getter SHOULD be mapped
		// Assuming it's the getter - based on name, access, descriptor and instructions - matches decompilers'
		// assumptions.
		// Decompilers assume it's a default getter and hide it, so we propose a name to prevent un-completable stats.
		final EntryMapping fakeGetterMappings = project.getRemapper().getMapping(fakeGetter);
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, fakeGetterMappings.tokenType());
		Assertions.assertEquals(targetName, fakeGetterMappings.targetName());
		Assertions.assertEquals(RecordComponentProposalService.ID, fakeGetterMappings.sourcePluginId());

		// real getter should NOT be mapped
		final EntryMapping componentGetterMapping = project.getRemapper().getMapping(componentGetter);
		Assertions.assertEquals(TokenType.OBFUSCATED, componentGetterMapping.tokenType());
	}

	@Test
	void testBridgeRecord() {
		final String doubleDesc = "Ljava/lang/Double;";
		final String stringGetterDesc = "()" + doubleDesc;

		final ClassEntry bridgeRecord = TestEntryFactory.newClass("f");
		final FieldEntry getField = TestEntryFactory.newField(bridgeRecord, "a", doubleDesc);
		// once Supplier is indexed as a lib, this should be named get
		final MethodEntry getGetter = TestEntryFactory.newMethod(bridgeRecord, "a", stringGetterDesc);
		final MethodEntry getBridge = TestEntryFactory.newMethod(bridgeRecord, "get", "()Ljava/lang/Object;");

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(getField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(getGetter).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(getBridge).tokenType());

		final String targetName = "mapped";
		project.getRemapper().putMapping(TestUtil.newVC(), getField, new EntryMapping(targetName));

		final EntryMapping fieldMapping = project.getRemapper().getMapping(getField);
		Assertions.assertSame(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals(targetName, fieldMapping.targetName());

		// getter should be mapped; it should be the only getter candidate
		final EntryMapping getterMapping = project.getRemapper().getMapping(getGetter);
		Assertions.assertSame(TokenType.DYNAMIC_PROPOSED, getterMapping.tokenType());
		Assertions.assertEquals(targetName, getterMapping.targetName());
		Assertions.assertEquals(RecordComponentProposalService.ID, getterMapping.sourcePluginId());

		// bridge should not be mapped; it should not be a getter candidate because
		// it has the wrong access and descriptor
		final EntryMapping bridgeMapping = project.getRemapper().getMapping(getBridge);
		Assertions.assertEquals(TokenType.OBFUSCATED, bridgeMapping.tokenType());
	}

	@Test
	void testIllegalGetterNameExclusion() {
		final String stringDesc = "Ljava/lang/String;";
		final String stringGetterDesc = "()" + stringDesc;

		final ClassEntry stringComponentOverrideGetterRecord = TestEntryFactory.newClass("i");
		final FieldEntry stringField = TestEntryFactory.newField(stringComponentOverrideGetterRecord, "a", stringDesc);
		final MethodEntry stringGetter = TestEntryFactory
				.newMethod(stringComponentOverrideGetterRecord, "a", stringGetterDesc);
		final MethodEntry toString = TestEntryFactory
				 .newMethod(stringComponentOverrideGetterRecord, "toString", stringGetterDesc);

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(stringField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(stringGetter).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(toString).tokenType());

		final String targetName = "mapped";
		project.getRemapper().putMapping(TestUtil.newVC(), stringField, new EntryMapping(targetName));

		final EntryMapping fieldMapping = project.getRemapper().getMapping(stringField);
		Assertions.assertSame(TokenType.DEOBFUSCATED, fieldMapping.tokenType());
		Assertions.assertEquals(targetName, fieldMapping.targetName());

		// getter should be mapped; it should be the only getter candidate: toString should be excluded from candidates
		// because its name is not a legal component name
		final EntryMapping getterMapping = project.getRemapper().getMapping(stringGetter);
		Assertions.assertSame(TokenType.DYNAMIC_PROPOSED, getterMapping.tokenType());
		Assertions.assertEquals(targetName, getterMapping.targetName());
		Assertions.assertEquals(RecordComponentProposalService.ID, getterMapping.sourcePluginId());

		// toString should not be mapped because it's name doesn't match the field,
		// its name is not a legal component name, and it's a library method (unmappable)
		final EntryMapping bridgeMapping = project.getRemapper().getMapping(toString);
		Assertions.assertEquals(TokenType.OBFUSCATED, bridgeMapping.tokenType());
	}

	@Test
	void testRecordComponentMappingRemoval() {
		ClassEntry aClass = TestEntryFactory.newClass("a");
		FieldEntry aField = TestEntryFactory.newField(aClass, "a", "I");
		MethodEntry aGetter = TestEntryFactory.newMethod(aClass, "a", "()I");

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aGetter).tokenType());

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

		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aField).tokenType());
		Assertions.assertSame(TokenType.OBFUSCATED, project.getRemapper().getMapping(aGetter).tokenType());

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

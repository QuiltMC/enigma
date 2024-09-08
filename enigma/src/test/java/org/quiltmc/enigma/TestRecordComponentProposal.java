package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

	@Test
	void testRecordComponentProposal() throws IOException {
		// todo add indexer and tests
		Reader r = new StringReader("""
				{
					"services": {
						"name_proposal": [
							{
								"id": "enigma:record_component_proposer"
							}
						]
					}
				}""");

		EnigmaProfile profile = EnigmaProfile.parse(r);
		Enigma enigma = Enigma.builder().setProfile(profile).build();
		EnigmaProject project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());

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
}

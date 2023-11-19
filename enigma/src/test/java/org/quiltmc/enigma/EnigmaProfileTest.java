package org.quiltmc.enigma;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.DecompilerService;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.util.Either;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

public class EnigmaProfileTest {
	@Test
	public void testParse() {
		Reader r = new StringReader("""
				{
					"mapping_save_parameters": {
						"file_name_format": "by_deobf"
					},
					"services": {
						"decompiler": {
							"id": "enigma:vineflower"
						},
						"jar_indexer": {
							"id": "enigma:dummy",
							"args": {
								"abc": "hello world",
								"foo": ["lorem", "ipsum", "dolor", "sit", "amet"]
							}
						},
						"name_proposal": [
							{
								"id": "enigma:enum_name_proposer"
							},
							{
								"id": "enigma:specialized_method_name_proposer"
							},
							{
								"id": "enigma:foo",
								"args": {
									"base": "org/quiltmc",
									"packages": ["foo", "bar"],
									"example": "abc, def, ghi"
								}
							}
						]
					}
				}""");
		EnigmaProfile profile = EnigmaProfile.parse(r);

		Assertions.assertEquals(MappingFileNameFormat.BY_DEOBF, profile.getMappingSaveParameters().fileNameFormat());

		List<EnigmaProfile.Service> decompilers = profile.getServiceProfiles(DecompilerService.TYPE);
		Assertions.assertEquals(1, decompilers.size());
		Assertions.assertTrue(decompilers.get(0).matches("enigma:vineflower"));

		List<EnigmaProfile.Service> indexers = profile.getServiceProfiles(JarIndexerService.TYPE);
		Assertions.assertEquals(1, indexers.size());
		Optional<String> abc = indexers.get(0).getArgument("abc").flatMap(e -> e.left());
		Assertions.assertTrue(abc.isPresent());
		Assertions.assertEquals("hello world", abc.get());
		Optional<List<String>> foo = indexers.get(0).getArgument("foo").flatMap(e -> e.right());
		Assertions.assertTrue(foo.isPresent());
		Assertions.assertEquals(List.of("lorem", "ipsum", "dolor", "sit", "amet"), foo.get());

		List<EnigmaProfile.Service> nameProposers = profile.getServiceProfiles(NameProposalService.TYPE);
		Assertions.assertEquals(3, nameProposers.size());
		EnigmaProfile.Service fooService = nameProposers.get(2);
		Assertions.assertTrue(fooService.getArgument("example").map(Either::isLeft).orElse(false));
	}

	@Test
	public void testMalformedJson() {
		Reader r = new StringReader("""
				{
					"q" "w"
				}""");

		Assertions.assertThrows(JsonSyntaxException.class, () -> EnigmaProfile.parse(r));
	}
}

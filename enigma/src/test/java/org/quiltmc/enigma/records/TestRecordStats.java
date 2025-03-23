package org.quiltmc.enigma.records;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.stats.StatsResult;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestRecordStats {
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
	void testParameters() {
		StatsResult stats = new StatsGenerator(project).generate(TestEntryFactory.newClass("c"), new GenerationParameters(EnumSet.of(StatType.PARAMETERS)));

		// total params in the class are 10
		// equals method is ignored
		// canonical constructor is ignored
		// remaining parameters should be 5
		assertThat(stats.getMappable(StatType.PARAMETERS), equalTo(5));
		assertThat(stats.getMapped(StatType.PARAMETERS), equalTo(0));
	}

	@Test
	void testMethods() {
		ClassEntry c = TestEntryFactory.newClass("c");
		StatsResult stats = new StatsGenerator(project).generate(c, new GenerationParameters(EnumSet.of(StatType.METHODS)));

		// 4 mappable methods: 1 for each field
		assertThat(stats.getMappable(StatType.METHODS), equalTo(4));
		assertThat(stats.getMapped(StatType.METHODS), equalTo(0));

		project.getRemapper().putMapping(TestUtil.newVC(), TestEntryFactory.newField(c, "a", "Ljava/lang/String;"), new EntryMapping("gaming"));
		StatsResult stats2 = new StatsGenerator(project).generate(c, new GenerationParameters(EnumSet.of(StatType.METHODS)));

		// 1 method mapped to match field
		assertThat(stats2.getMappable(StatType.METHODS), equalTo(4));
		assertThat(stats2.getMapped(StatType.METHODS), equalTo(1));
	}
}

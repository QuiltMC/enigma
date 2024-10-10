package org.quiltmc.enigma.stats;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.stats.StatsResult;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class TestStatGenerationInheritance {
	private static final Path JAR = TestUtil.obfJar("interfaces");
	private static EnigmaProject project;

	@BeforeEach
	void openProject() {
		try {
			Enigma enigma = Enigma.create();
			project = enigma.openJar(JAR, new JarClassProvider(JAR), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testInterfacePropagation() {
		// https://github.com/QuiltMC/enigma/issues/230 - verifying backend bit
		ClassEntry interfaceEntry = TestEntryFactory.newClass("b");
		ClassEntry inheritorEntry = TestEntryFactory.newClass("a");

		StatsResult interfaceResult = new StatsGenerator(project).generate(Set.of(StatType.METHODS), interfaceEntry, false);
		Assertions.assertEquals(2, interfaceResult.getMappable());

		// the inheritor does not own the method; it won't count towards its stats
		StatsResult inheritorResult = new StatsGenerator(project).generate(Set.of(StatType.METHODS), inheritorEntry, false);
		Assertions.assertEquals(0, inheritorResult.getMappable());

		MethodEntry inheritedMethod = TestEntryFactory.newMethod(inheritorEntry, "a", "(D)D");
		project.getRemapper().putMapping(TestUtil.newVC(), inheritedMethod, new EntryMapping("mapped"));

		StatsResult interfaceResult2 = new StatsGenerator(project).generate(Set.of(StatType.METHODS), interfaceEntry, false);
		Assertions.assertEquals(1, interfaceResult2.getMapped());
	}
}

package org.quiltmc.enigma;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestJarIndexEnums {
	public static final Path JAR = TestUtil.obfJar("enums");

	@Test
	void checkEnumStats() {
		EnigmaProject project = openProject();
		ProjectStatsResult stats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), null, new GenerationParameters(EnumSet.allOf(StatType.class)));

		assertThat(stats.getMapped(StatType.CLASSES), equalTo(0));
		assertThat(stats.getMapped(StatType.FIELDS), equalTo(0));
		assertThat(stats.getMapped(StatType.METHODS), equalTo(0));
		assertThat(stats.getMapped(StatType.PARAMETERS), equalTo(0));

		assertThat(stats.getMappable(StatType.CLASSES), equalTo(3));
		assertThat(stats.getMappable(StatType.FIELDS), equalTo(17));
		assertThat(stats.getMappable(StatType.METHODS), equalTo(4));
		assertThat(stats.getMappable(StatType.PARAMETERS), equalTo(3));
	}

	private static EnigmaProject openProject() {
		try {
			Enigma enigma = Enigma.create();
			return enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

package org.quiltmc.enigma;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestInnerClassParameterStats {
	private static final Path JAR = TestUtil.obfJar("inner_classes");

	/**
	 * Note: this test will break when parameters are added/removed from any input in the {@code inner_classes} package or the {@code Keep} class.
	 * This is ok! Simply change the assertions for the new values.
	 */
	@Test
	public void testInnerClassParameterStats() {
		EnigmaProject project = openProject();
		ProjectStatsResult stats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), null, new GenerationParameters(EnumSet.of(StatType.PARAMETERS)));
		// 8/13 total parameters in our six classes are non-mappable, meaning that we should get 0/3 parameters mapped
		// these non-mappable parameters come from non-static inner classes taking their enclosing class as a parameter
		// they are currently manually excluded by a check in the stats generator
		assertThat(stats.getMappable(StatType.PARAMETERS), equalTo(5));
		assertThat(stats.getMapped(StatType.PARAMETERS), equalTo(0));
	}

	private static EnigmaProject openProject() {
		try {
			Enigma enigma = Enigma.create();
			return enigma.openJar(JAR, new JarClassProvider(JAR), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

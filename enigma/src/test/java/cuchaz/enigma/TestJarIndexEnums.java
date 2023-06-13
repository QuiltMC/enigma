package cuchaz.enigma;

import org.junit.jupiter.api.Test;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.stats.StatType;
import cuchaz.enigma.stats.StatsGenerator;
import cuchaz.enigma.stats.StatsResult;

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
		StatsResult stats = new StatsGenerator(project).generate(ProgressListener.none(), EnumSet.allOf(StatType.class), "", false);

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
			return enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

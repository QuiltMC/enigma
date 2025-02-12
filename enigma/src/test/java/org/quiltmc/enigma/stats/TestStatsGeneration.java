package org.quiltmc.enigma.stats;

import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.util.EntryUtil;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestStatsGeneration {
	private static final Path JAR = TestUtil.obfJar("complete");

	@Test
	void checkNoMappedEntriesByDefault() {
		EnigmaProject project = openProject();
		ProjectStatsResult stats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), null, new GenerationParameters(EnumSet.of(StatType.METHODS)));
		assertThat(stats.getMapped(), equalTo(0));
		assertThat(stats.getPercentage(), equalTo(0d));
	}

	@Test
	void checkClassMapping() {
		EnigmaProject project = openProject();
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getClasses());
		checkFullyMapped(project, StatType.CLASSES);
	}

	@Test
	void checkMethodMapping() {
		EnigmaProject project = openProject();
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getMethods());
		checkFullyMapped(project, StatType.METHODS);
	}

	@Test
	void checkFieldMapping() {
		EnigmaProject project = openProject();
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getFields());
		checkFullyMapped(project, StatType.FIELDS);
	}

	@Test
	void checkOverallMapping() {
		// note: does not check parameters. as this is the most fragile part of stats generation, it should be added to this test as soon as possible!
		EnigmaProject project = openProject();
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getClasses());
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getFields());
		renameAll(project, project.getJarIndex().getIndex(EntryIndex.class).getMethods());
		checkFullyMapped(project, StatType.METHODS, StatType.CLASSES, StatType.FIELDS);
	}

	private static void renameAll(EnigmaProject project, Collection<? extends Entry<?>> entries) {
		int i = 0;

		for (Entry<?> entry : entries) {
			EntryChange<? extends Entry<?>> change = EntryChange.modify(entry).withDeobfName("a" + i);

			EntryMapping prev = project.getRemapper().getMapping(entry);
			EntryMapping mapping = EntryUtil.applyChange(prev, change);

			project.getRemapper().putMapping(new ValidationContext(null), entry, mapping);
			i++;
		}
	}

	private static EnigmaProject openProject() {
		try {
			Enigma enigma = Enigma.create();
			return enigma.openJar(JAR, new JarClassProvider(JAR), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void checkFullyMapped(EnigmaProject project, StatType... types) {
		ProjectStatsResult stats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), null, new GenerationParameters(Set.of(types)));
		assertThat(stats.getMapped(types), equalTo(stats.getMappable(types)));
		assertThat(stats.getPercentage(types), equalTo(100d));
	}
}

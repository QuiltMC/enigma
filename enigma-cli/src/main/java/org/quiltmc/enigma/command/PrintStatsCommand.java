package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.quiltmc.enigma.command.CommonArguments.ENIGMA_PROFILE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;

public final class PrintStatsCommand extends Command {
	public static final PrintStatsCommand INSTANCE = new PrintStatsCommand();

	private PrintStatsCommand() {
		super(
				ImmutableList.of(INPUT_JAR, INPUT_MAPPINGS),
				ImmutableList.of(ENIGMA_PROFILE)
		);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws Exception {
		run(
				getReadablePath(args.get(INPUT_JAR.getName())),
				getReadablePath(INPUT_MAPPINGS.getName()),
				getReadablePath(ENIGMA_PROFILE.getName()),
				null
		);
	}

	@Override
	public String getName() {
		return "print-stats";
	}

	@Override
	public String getDescription() {
		return "Generates and prints out the statistics of how well the provided mappings cover the provided JAR file.";
	}

	public static void run(Path inJar, Path mappings, @Nullable Path profilePath, @Nullable Iterable<EnigmaPlugin> plugins) throws Exception {
		EnigmaProfile profile = EnigmaProfile.read(profilePath);
		Enigma enigma = createEnigma(profile, plugins);

		run(inJar, mappings, enigma);
	}

	public static void run(Path inJar, Path mappings, Enigma enigma) throws Exception {
		StatsGenerator generator = new StatsGenerator(openProject(inJar, mappings, enigma));
		ProjectStatsResult result = generator.generate(new ConsoleProgressListener(), new GenerationParameters(Set.of(StatType.values())));

		Logger.info(String.format("Overall mapped: %.2f%% (%s / %s)", result.getPercentage(), result.getMapped(), result.getMappable()));
		Logger.info(String.format("Classes: %.2f%% (%s / %s)", result.getPercentage(StatType.CLASSES), result.getMapped(StatType.CLASSES), result.getMappable(StatType.CLASSES)));
		Logger.info(String.format("Fields: %.2f%% (%s / %s)", result.getPercentage(StatType.FIELDS), result.getMapped(StatType.FIELDS), result.getMappable(StatType.FIELDS)));
		Logger.info(String.format("Methods: %.2f%% (%s / %s)", result.getPercentage(StatType.METHODS), result.getMapped(StatType.METHODS), result.getMappable(StatType.METHODS)));
		Logger.info(String.format("Parameters: %.2f%% (%s / %s)", result.getPercentage(StatType.PARAMETERS), result.getMapped(StatType.PARAMETERS), result.getMappable(StatType.PARAMETERS)));
	}
}


package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.command.PrintStatsCommand.Required;
import org.quiltmc.enigma.command.PrintStatsCommand.Optional;
import org.quiltmc.enigma.util.I18n;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.quiltmc.enigma.command.CommonArguments.ENIGMA_PROFILE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;

public final class PrintStatsCommand extends Command<Required, Optional> {
	private static final Argument<Set<StatType>> INCLUDED_TYPES = Argument.ofCollection(
			"included-types", Argument.alternativesOf(StatType.class, "&|"),
			"""
				The stat types to include.""",
			string -> Argument.parseCaseInsensitiveEnum(StatType.class, string),
			Collectors.toUnmodifiableSet()
	);

	private static final Argument<Boolean> INCLUDE_SYNTHETIC = Argument.ofBool("include-synthetic",
			"""
				Whether to include synthetic entries."""
	);

	private static final Argument<Boolean> COUNT_FALLBACK = Argument.ofBool("count-fallback",
			"""
				Whether to count fallback-proposed entries as mapped."""
	);

	public static final PrintStatsCommand INSTANCE = new PrintStatsCommand();

	private PrintStatsCommand() {
		super(
				ArgsParser.of(INPUT_JAR, INPUT_MAPPINGS, Required::new),
				ArgsParser.of(ENIGMA_PROFILE, INCLUDED_TYPES, INCLUDE_SYNTHETIC, COUNT_FALLBACK, Optional::new)
		);
	}

	@Override
	void runImpl(Required required, Optional optional) throws Exception {
		final Set<StatType> includedTypes = optional.includedTypes == null || optional.includedTypes.isEmpty()
				? EnumSet.allOf(StatType.class)
				: optional.includedTypes;

		final GenerationParameters params = new GenerationParameters(
					includedTypes,
					Boolean.TRUE.equals(optional.includeSynthetic),
					Boolean.TRUE.equals(optional.countFallback)
		);

		run(required.inputJar, required.inputMappings, optional.enigmaProfile, params, null);
	}

	@Override
	public String getName() {
		return "print-stats";
	}

	@Override
	public String getDescription() {
		return "Generates and prints out the statistics of how well the provided mappings cover the provided JAR file.";
	}

	public static void run(
			Path inJar, Path mappings, @Nullable Path profilePath,
			@Nullable GenerationParameters params, @Nullable Iterable<EnigmaPlugin> plugins
	) throws Exception {
		EnigmaProfile profile = EnigmaProfile.read(profilePath);
		Enigma enigma = createEnigma(profile, plugins);

		run(inJar, mappings, enigma, params);
	}

	public static void run(Path inJar, Path mappings, Enigma enigma, @Nullable GenerationParameters params) throws Exception {
		StatsGenerator generator = new StatsGenerator(openProject(inJar, mappings, enigma));

		if (params == null) {
			params = new GenerationParameters();
		}

		ProjectStatsResult result = generator.generate(new ConsoleProgressListener(), params);

		final Set<StatType> includedTypes = params.includedTypes();

		if (includedTypes.size() > 1) {
			final String overall = I18n.translate("menu.file.stats.overall");
			logResult(overall, result.getPercentage(), result.getMapped(), result.getMappable());
		}

		for (final StatType type : StatType.values()) {
			if (includedTypes.contains(type)) {
				logResult(type.getName(), result.getPercentage(type), result.getMapped(type), result.getMappable(type));
			}
		}
	}

	private static void logResult(String label, double percentage, int mapped, int mappable) {
		Logger.info("%s: %.2f%% (%s / %s)".formatted(label, percentage, mapped, mappable));
	}

	record Required(Path inputJar, Path inputMappings) { }
	record Optional(Path enigmaProfile, Set<StatType> includedTypes, Boolean includeSynthetic, Boolean countFallback) { }
}


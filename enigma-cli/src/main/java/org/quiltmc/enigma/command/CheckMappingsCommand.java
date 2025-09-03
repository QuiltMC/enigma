package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.PackageVisibilityIndex;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.command.ArgsParser.Empty;
import org.quiltmc.enigma.command.CheckMappingsCommand.Required;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;

public final class CheckMappingsCommand extends Command<Required, Empty> {
	public static final CheckMappingsCommand INSTANCE = new CheckMappingsCommand();

	private CheckMappingsCommand() {
		super(ArgsParser.of(INPUT_JAR, INPUT_MAPPINGS, Required::new), Empty.PARSER);
	}

	@Override
	void runImpl(Required required, Empty optional) throws Exception {
		run(required.inputJar, required.inputMappings);
	}

	@Override
	public String getName() {
		return "check-mappings";
	}

	@Override
	public String getDescription() {
		return "Checks that the mappings can be applied on the jar and used without any runtime errors, such as package-private members accessed from an invalid package.";
	}

	public static void run(Path fileJarIn, Path fileMappings) throws Exception {
		EnigmaProject project = openProject(fileJarIn, fileMappings);
		JarIndex idx = project.getJarIndex();

		boolean error = false;

		for (Set<ClassEntry> partition : idx.getIndex(PackageVisibilityIndex.class).getPartitions()) {
			long packages = partition.stream()
					.map(project.getRemapper()::deobfuscate)
					.map(ClassEntry::getPackageName)
					.distinct()
					.count();
			if (packages > 1) {
				error = true;
				Logger.error("Must be in one package:\n{}", () -> partition.stream()
						.map(project.getRemapper()::deobfuscate)
						.map(ClassEntry::toString)
						.sorted()
						.collect(Collectors.joining("\n"))
				);
			}
		}

		if (error) {
			throw new IllegalStateException("Errors in package visibility detected, see error logged above!");
		}
	}

	record Required(Path inputJar, Path inputMappings) { }
}

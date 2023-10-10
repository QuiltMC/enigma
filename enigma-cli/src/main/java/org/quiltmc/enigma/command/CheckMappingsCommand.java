package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.PackageVisibilityIndex;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckMappingsCommand extends Command {
	public CheckMappingsCommand() {
		super(Argument.INPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.required());
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileJarIn = getReadableFile(this.getArg(args, 0)).toPath();
		Path fileMappings = getReadablePath(this.getArg(args, 1));
		run(fileJarIn, fileMappings);
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
					.map(project.getMapper()::deobfuscate)
					.map(ClassEntry::getPackageName)
					.distinct()
					.count();
			if (packages > 1) {
				error = true;
				Logger.error("Must be in one package:\n{}", () -> partition.stream()
						.map(project.getMapper()::deobfuscate)
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
}

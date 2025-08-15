package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.command.DeobfuscateCommand.Required;

import java.nio.file.Path;

import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.OUTPUT_JAR;

public final class DeobfuscateCommand extends Command<Required, Path> {
	public static final DeobfuscateCommand INSTANCE = new DeobfuscateCommand();

	private DeobfuscateCommand() {
		super(
				ArgsParser.of(INPUT_JAR, OUTPUT_JAR, Required::new),
				ArgsParser.of(INPUT_MAPPINGS)
		);
	}

	@Override
	void runImpl(Required required, Path inputMappings) throws Exception {
		run(required.inputJar, required.outputJar, inputMappings);
	}

	@Override
	public String getName() {
		return "deobfuscate";
	}

	@Override
	public String getDescription() {
		return "Remaps all names in the jar according to the provided mappings.";
	}

	public static void run(Path fileJarIn, Path fileJarOut, Path fileMappings) throws Exception {
		EnigmaProject project = openProject(fileJarIn, fileMappings);

		ProgressListener progress = new ConsoleProgressListener();

		EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
		jar.write(fileJarOut, progress);
	}

	record Required(Path inputJar, Path outputJar) { }
}

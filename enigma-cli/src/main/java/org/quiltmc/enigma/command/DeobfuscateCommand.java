package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;

import java.nio.file.Path;
import java.util.Map;

import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.OUTPUT_JAR;

public final class DeobfuscateCommand extends Command {
	public static final DeobfuscateCommand INSTANCE = new DeobfuscateCommand();

	private DeobfuscateCommand() {
		super(
				ImmutableList.of(INPUT_JAR, OUTPUT_JAR),
				ImmutableList.of(INPUT_MAPPINGS)
		);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws Exception {
		run(
				getReadablePath(args.get(INPUT_JAR.getName())),
				getWritableFile(args.get(OUTPUT_JAR.getName())).toPath(),
				getReadablePath(args.get(INPUT_MAPPINGS.getName()))
		);
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
}

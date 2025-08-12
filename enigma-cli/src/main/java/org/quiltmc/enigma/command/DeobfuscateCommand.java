package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;

import java.nio.file.Path;

public final class DeobfuscateCommand extends Command {
	public DeobfuscateCommand() {
		super(
				ImmutableList.of(CommonArguments.INPUT_JAR, CommonArguments.OUTPUT_JAR),
				ImmutableList.of(CommonArguments.INPUT_MAPPINGS)
		);
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileJarIn = getReadablePath(this.getArg(args, 0));
		Path fileJarOut = getWritableFile(this.getArg(args, 1)).toPath();
		Path fileMappings = getReadablePath(this.getArg(args, 2));

		run(fileJarIn, fileJarOut, fileMappings);
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

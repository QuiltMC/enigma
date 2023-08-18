package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;

import java.nio.file.Path;

public class DeobfuscateCommand extends Command {
	public DeobfuscateCommand() {
		super("deobfuscate",
				Argument.INPUT_JAR.required(),
				Argument.OUTPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.optional());
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileJarIn = getReadablePath(getArg(args, 0, "in jar", true));
		Path fileJarOut = getWritableFile(getArg(args, 1, "out jar", true)).toPath();
		Path fileMappings = getReadablePath(getArg(args, 2, "mappings file", false));

		run(fileJarIn, fileJarOut, fileMappings);
	}

	public static void run(Path fileJarIn, Path fileJarOut, Path fileMappings) throws Exception {
		EnigmaProject project = openProject(fileJarIn, fileMappings);

		ProgressListener progress = new ConsoleProgressListener();

		EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
		jar.write(fileJarOut, progress);
	}
}

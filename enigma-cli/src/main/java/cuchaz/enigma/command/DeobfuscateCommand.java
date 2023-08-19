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
		Path fileJarIn = getReadablePath(this.getArg(args, 0));
		Path fileJarOut = getWritableFile(this.getArg(args, 1)).toPath();
		Path fileMappings = getReadablePath(this.getArg(args, 2));

		run(fileJarIn, fileJarOut, fileMappings);
	}

	public static void run(Path fileJarIn, Path fileJarOut, Path fileMappings) throws Exception {
		EnigmaProject project = openProject(fileJarIn, fileMappings);

		ProgressListener progress = new ConsoleProgressListener();

		EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
		jar.write(fileJarOut, progress);
	}
}

package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;

import java.nio.file.Path;

public class DeobfuscateCommand extends Command {
	public DeobfuscateCommand() {
		super(Argument.INPUT_JAR.required(),
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

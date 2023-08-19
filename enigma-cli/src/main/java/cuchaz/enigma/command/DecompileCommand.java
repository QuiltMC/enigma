package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.EnigmaProject.DecompileErrorStrategy;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Decompilers;
import org.tinylog.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;

public class DecompileCommand extends Command {
	public DecompileCommand() {
		super(Argument.DECOMPILER.required(),
				Argument.INPUT_JAR.required(),
				Argument.OUTPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.optional());
	}

	@Override
	public void run(String... args) throws Exception {
		String decompilerName = this.getArg(args, 0);
		Path fileJarIn = getReadableFile(this.getArg(args, 1)).toPath();
		Path fileJarOut = getWritableFolder(this.getArg(args, 2)).toPath();
		Path fileMappings = getReadablePath(this.getArg(args, 3));

		run(decompilerName, fileJarIn, fileJarOut, fileMappings);
	}

	@Override
	public String getName() {
		return "decompile";
	}

	@Override
	public String getDescription() {
		return "Decompiles the provided jar into human-readable code.";
	}

	public static void run(String decompilerName, Path fileJarIn, Path fileJarOut, Path fileMappings) throws Exception {
		DecompilerService decompilerService;

		try {
			Field decompilerField = Decompilers.class.getField(decompilerName.toUpperCase(Locale.ROOT));
			decompilerService = (DecompilerService) decompilerField.get(null);
		} catch (NoSuchFieldException e) {
			Logger.error("Decompiler not found.");
			return;
		}

		EnigmaProject project = openProject(fileJarIn, fileMappings);

		ProgressListener progress = new ConsoleProgressListener();

		EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
		EnigmaProject.SourceExport source = jar.decompile(progress, decompilerService, DecompileErrorStrategy.TRACE_AS_SOURCE);

		source.write(fileJarOut, progress);
	}
}

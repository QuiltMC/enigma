package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.EnigmaProject.DecompileErrorStrategy;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.tinylog.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;

public final class DecompileCommand extends Command {
	private static final Argument DECOMPILER = new Argument("<decompiler>",
			"""
					The decompiler to use when producing output. Allowed values are (case-insensitive):
					- VINEFLOWER
					- CFR
					- PROCYON
					- BYTECODE"""
	);

	public DecompileCommand() {
		super(
				ImmutableList.of(DECOMPILER, CommonArguments.INPUT_JAR, CommonArguments.OUTPUT_JAR),
				ImmutableList.of(CommonArguments.INPUT_MAPPINGS)
		);
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

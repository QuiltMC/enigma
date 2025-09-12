package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.EnigmaProject.DecompileErrorStrategy;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.command.DecompileCommand.Required;
import org.tinylog.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;

public final class DecompileCommand extends Command<Required, Path> {
	private static final Argument<String> DECOMPILER = Argument.ofLenientEnum("decompiler", Decompiler.class,
			"""
					The decompiler to use when producing output. Allowed values are (case-insensitive):"""
				+ Decompiler.VALUES.stream()
					.map(Object::toString)
					.map(decompiler -> "\n- " + decompiler)
					.collect(Collectors.joining())
	);

	private static final Argument<Path> OUTPUT_DIR = Argument.ofFolder("output-dir",
			"""
				The directory to save decompiler output to.
				"""
	);

	public static final DecompileCommand INSTANCE = new DecompileCommand();

	private DecompileCommand() {
		super(
				ArgsParser.of(DECOMPILER, INPUT_JAR, OUTPUT_DIR, Required::new),
				ArgsParser.of(INPUT_MAPPINGS)
		);
	}

	@Override
	void runImpl(Required required, Path inputMappings) throws Exception {
		run(required.decompiler, required.inputJar, required.outputDir, inputMappings);
	}

	@Override
	public String getName() {
		return "decompile";
	}

	@Override
	public String getDescription() {
		return "Decompiles the provided jar into human-readable code.";
	}

	public static void run(Decompiler decompiler, Path fileJarIn, Path fileJarOut, Path fileMappings) throws Exception {
		run(decompiler.toString(), fileJarIn, fileJarOut, fileMappings);
	}

	public static void run(String decompilerName, Path fileJarIn, Path outputDir, Path fileMappings) throws Exception {
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

		source.write(outputDir, progress);
	}

	public enum Decompiler {
		VINEFLOWER, CFR, PROCYON, BYTECODE;

		public static final ImmutableList<Decompiler> VALUES = ImmutableList.copyOf(values());
	}

	record Required(String decompiler, Path inputJar, Path outputDir) { }
}

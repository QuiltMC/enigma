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
import java.util.Map;
import java.util.stream.Collectors;

import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.OUTPUT_JAR;

public final class DecompileCommand extends Command {
	private static final Argument<String> DECOMPILER = Argument.ofLenientEnum("decompiler", Decompiler.class,
			"""
					The decompiler to use when producing output. Allowed values are (case-insensitive):"""
				+ Decompiler.VALUES.stream()
					.map(Object::toString)
					.map(decompiler -> "\n- " + decompiler)
					.collect(Collectors.joining())
	);

	public static final DecompileCommand INSTANCE = new DecompileCommand();

	private DecompileCommand() {
		super(
				ImmutableList.of(DECOMPILER, INPUT_JAR, OUTPUT_JAR),
				ImmutableList.of(INPUT_MAPPINGS)
		);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws Exception {
		run(DECOMPILER.get(args), INPUT_JAR.get(args), OUTPUT_JAR.get(args), INPUT_MAPPINGS.get(args));
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

	public enum Decompiler {
		VINEFLOWER, CFR, PROCYON, BYTECODE;

		public static final ImmutableList<Decompiler> VALUES = ImmutableList.copyOf(values());
	}
}

package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class DropInvalidMappingsCommand extends Command {
	public static final DropInvalidMappingsCommand INSTANCE = new DropInvalidMappingsCommand();

	private DropInvalidMappingsCommand() {
		super(
				ImmutableList.of(CommonArguments.INPUT_JAR, CommonArguments.INPUT_MAPPINGS),
				ImmutableList.of(CommonArguments.MAPPING_OUTPUT)
		);
	}

	@Override
	public void run(String... args) throws Exception {
		Path jarIn = getReadablePath(this.getArg(args, 0));
		Path mappingsIn = getReadablePath(this.getArg(args, 1));
		String mappingsOutArg = this.getArg(args, 2);
		Path mappingsOut = mappingsOutArg != null && !mappingsOutArg.isEmpty() ? getReadablePath(mappingsOutArg) : mappingsIn;

		run(jarIn, mappingsIn, mappingsOut);
	}

	@Override
	public String getName() {
		return "drop-invalid-mappings";
	}

	@Override
	public String getDescription() {
		return "Removes all invalid mapping entries (entries whose obfuscated name is not found in the jar) and empty mappings (garbage lines that don't add anything to the mappings) from the provided mappings.";
	}

	public static void run(Path jarIn, Path mappingsIn, Path mappingsOut) throws Exception {
		if (mappingsIn == null) {
			Logger.warn("No mappings input specified, skipping.");
			return;
		}

		MappingsWriter writer = CommandsUtil.getWriter(createEnigma(), mappingsIn);
		EnigmaProject project = openProject(jarIn, mappingsIn);

		Logger.info("Dropping invalid mappings...");

		var droppedMappings = project.dropMappings(ProgressListener.createEmpty());

		if (!droppedMappings.isEmpty()) {
			Logger.info("Found and dropped {} invalid mappings.", droppedMappings.size());
			Logger.info("Writing mappings...");

			if (mappingsOut == mappingsIn) {
				Logger.info("Overwriting input mappings");
				Files.walkFileTree(mappingsIn, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}
				});

				Files.deleteIfExists(mappingsIn);
			}

			MappingSaveParameters saveParameters = project.getEnigma().getProfile().getMappingSaveParameters();
			writer.write(project.getRemapper().getMappings(), mappingsOut, ProgressListener.createEmpty(), saveParameters);
		} else {
			Logger.info("No invalid mappings found.");
		}
	}
}

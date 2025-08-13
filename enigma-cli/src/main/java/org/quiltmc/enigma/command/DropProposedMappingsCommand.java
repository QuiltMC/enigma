package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
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

public class DropProposedMappingsCommand extends Command {
	public DropProposedMappingsCommand() {
		super(Argument.INPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.required(),
				Argument.ENIGMA_PROFILE.required(),
				Argument.MAPPING_OUTPUT.optional());
	}

	@Override
	public void run(String... args) throws Exception {
		Path jarIn = getReadablePath(this.getArg(args, 0));
		Path mappingsIn = getReadablePath(this.getArg(args, 1));
		Path profile = getReadablePath(this.getArg(args, 2));
		String mappingsOutArg = this.getArg(args, 3);
		Path mappingsOut = mappingsOutArg != null && !mappingsOutArg.isEmpty() ? getReadablePath(mappingsOutArg) : mappingsIn;

		run(jarIn, mappingsIn, profile, mappingsOut);
	}

	@Override
	public String getName() {
		return "drop-proposed-mappings";
	}

	@Override
	public String getDescription() {
		return "Removes all proposed mapping entries that are the same as manually written entries from the provided mappings.";
	}

	public static void run(Path jarIn, Path mappingsIn, Path profilePath, Path mappingsOut) throws Exception {
		if (mappingsIn == null) {
			Logger.warn("No mappings input specified, skipping.");
			return;
		}

		Enigma enigma = createEnigma(EnigmaProfile.read(profilePath), null);

		MappingsWriter writer = CommandsUtil.getWriter(enigma, mappingsIn);
		EnigmaProject project = openProject(jarIn, mappingsIn, enigma);

		Logger.info("Dropping proposed mappings...");

		var droppedMappings = project.dropProposedMappings(ProgressListener.createEmpty());

		if (!droppedMappings.isEmpty()) {
			Logger.info("Found and dropped {} duplicate mappings.", droppedMappings.size());
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
			Logger.info("No duplicate proposed mappings found.");
		}
	}
}

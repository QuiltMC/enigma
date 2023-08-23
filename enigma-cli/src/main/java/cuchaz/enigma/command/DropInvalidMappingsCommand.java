package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DropInvalidMappingsCommand extends Command {
	public DropInvalidMappingsCommand() {
		super(Argument.INPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.required(),
				Argument.MAPPING_OUTPUT.optional());
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
		return "Removes all invalid mapping entries (entries whose obfuscated name is not found in the jar) from the provided mappings.";
	}

	public static void run(Path jarIn, Path mappingsIn, Path mappingsOut) throws Exception {
		if (mappingsIn == null) {
			Logger.warn("No mappings input specified, skipping.");
			return;
		}

		MappingFormat format = MappingFormat.parseFromFile(mappingsIn);
		EnigmaProject project = openProject(jarIn, mappingsIn);

		Logger.info("Dropping invalid mappings...");

		project.dropMappings(ProgressListener.none());

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
		format.write(project.getMapper().getObfToDeobf(), mappingsOut, ProgressListener.none(), saveParameters);
	}
}

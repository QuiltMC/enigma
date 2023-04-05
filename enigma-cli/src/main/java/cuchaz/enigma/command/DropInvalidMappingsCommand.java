package cuchaz.enigma.command;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
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
		super("dropinvalidmappings");
	}

	@Override
	public String getUsage() {
		return "<in jar> <mappings in> [<mappings out>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 3;
	}

	@Override
	public void run(String... args) throws Exception {
		Path jarIn = getReadablePath(getArg(args, 0, "in jar", true));
		Path mappingsIn = getReadablePath(getArg(args, 1, "mappings in", true));
		String mappingsOutArg = getArg(args, 2, "mappings out", false);
		Path mappingsOut = mappingsOutArg != null && !mappingsOutArg.isEmpty() ? getReadablePath(mappingsOutArg) : mappingsIn;

		run(jarIn, mappingsIn, mappingsOut);
	}

	public static void run(Path jarIn, Path mappingsIn, Path mappingsOut) throws Exception {
		if (mappingsIn == null) {
			Logger.warn("No mappings input specified, skipping.");
			return;
		}

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
		writeMappings(project.getMapper().getObfToDeobf(), mappingsOut, ProgressListener.none(), saveParameters);
	}
}

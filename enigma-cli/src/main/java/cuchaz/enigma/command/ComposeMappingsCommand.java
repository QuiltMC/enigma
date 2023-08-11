package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.MappingOperations;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

public class ComposeMappingsCommand extends Command {
	public ComposeMappingsCommand() {
		super("compose-mappings");
	}

	@Override
	public String getUsage() {
		return "<left-format> <left> <right-format> <right> <result-format> <result> <keep-mode>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 5;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path left = getReadablePath(getArg(args, 0, "left", true));
		Path right = getReadablePath(getArg(args, 1, "right", true));
		String resultFormat = getArg(args, 2, "result-format", true);
		Path result = getWritablePath(getArg(args, 3, "result", true));
		String keepMode = getArg(args, 4, "keep-mode", true);

		run(left, right, resultFormat, result, keepMode);
	}

	public static void run(Path leftFile, Path rightFile, String resultFormat, Path resultFile, String keepMode) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		MappingFormat leftFormat = MappingFormat.parseFromFile(leftFile);
		EntryTree<EntryMapping> left = leftFormat.read(leftFile);
		MappingFormat rightFormat = MappingFormat.parseFromFile(rightFile);
		EntryTree<EntryMapping> right = rightFormat.read(rightFile);
		EntryTree<EntryMapping> result = MappingOperations.compose(left, right, keepMode.equals("left") || keepMode.equals("both"), keepMode.equals("right") || keepMode.equals("both"));

		Utils.delete(resultFile);
		MappingsWriter writer = MappingCommandsUtil.getWriter(resultFormat);
		writer.write(result, resultFile, ProgressListener.none(), saveParameters);
	}
}

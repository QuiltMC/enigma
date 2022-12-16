package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.MappingOperations;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
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
		return length == 7;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		String leftFormat = getArg(args, 0, "left-format", true);
		Path left = getReadablePath(getArg(args, 1, "left", true));
		String rightFormat = getArg(args, 2, "right-format", true);
		Path right = getReadablePath(getArg(args, 3, "right", true));
		String resultFormat = getArg(args, 4, "result-format", true);
		Path result = getWritablePath(getArg(args, 5, "result", true));
		String keepMode = getArg(args, 6, "keep-mode", true);

		run(leftFormat, left, rightFormat, right, resultFormat, result, keepMode);
	}

	public static void run(String leftFormat, Path leftFile, String rightFormat, Path rightFile, String resultFormat, Path resultFile, String keepMode) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		EntryTree<EntryMapping> left = MappingCommandsUtil.read(leftFormat, leftFile, saveParameters);
		EntryTree<EntryMapping> right = MappingCommandsUtil.read(rightFormat, rightFile, saveParameters);
		EntryTree<EntryMapping> result = MappingOperations.compose(left, right, keepMode.equals("left") || keepMode.equals("both"), keepMode.equals("right") || keepMode.equals("both"));

		Utils.delete(resultFile);
		MappingCommandsUtil.write(result, resultFormat, resultFile, saveParameters);
	}
}

package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

public class ConvertMappingsCommand extends Command {
	public ConvertMappingsCommand() {
		super("convert-mappings");
	}

	@Override
	public String getUsage() {
		return "<source-format> <source> <result-format> <result>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 4;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		String sourceFormat = getArg(args, 0, "source-format", true);
		Path source = getReadablePath(getArg(args, 1, "source", true));
		String resultFormat = getArg(args, 2, "result-format", true);
		Path result = getWritablePath(getArg(args, 3, "result", true));

		run(sourceFormat, source, resultFormat, result);
	}

	public static void run(String sourceFormat, Path source, String resultFormat, Path output) throws MappingParseException, IOException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		EntryTree<EntryMapping> mappings = MappingCommandsUtil.read(sourceFormat, source, saveParameters);

		Utils.delete(output);
		MappingCommandsUtil.write(mappings, resultFormat, output, saveParameters);
	}
}

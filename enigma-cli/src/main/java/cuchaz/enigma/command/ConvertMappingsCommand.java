package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
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

public class ConvertMappingsCommand extends Command {
	public ConvertMappingsCommand() {
		super("convert-mappings",
				Argument.INPUT_MAPPINGS.required(),
				Argument.OUTPUT_MAPPING_FORMAT.required(),
				Argument.MAPPING_OUTPUT.required());
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path source = getReadablePath(getArg(args, 0, "source", true));
		String resultFormat = getArg(args, 1, "result-format", true);
		Path result = getWritablePath(getArg(args, 2, "result", true));

		run(source, resultFormat, result);
	}

	public static void run(Path source, String resultFormat, Path output) throws MappingParseException, IOException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

		MappingFormat format = MappingFormat.parseFromFile(source);
		EntryTree<EntryMapping> mappings = format.read(source);

		Utils.delete(output);
		MappingsWriter writer = MappingCommandsUtil.getWriter(resultFormat);
		writer.write(mappings, output, ProgressListener.none(), saveParameters);
	}
}

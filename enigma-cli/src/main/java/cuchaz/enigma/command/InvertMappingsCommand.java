package cuchaz.enigma.command;

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

public class InvertMappingsCommand extends Command {
	public InvertMappingsCommand() {
		super(Argument.INPUT_MAPPINGS.required(),
				Argument.OUTPUT_MAPPING_FORMAT.required(),
				Argument.OUTPUT_FOLDER.required());
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path source = getReadablePath(this.getArg(args, 0));
		String resultFormat = this.getArg(args, 1);
		Path result = getWritablePath(this.getArg(args, 2));

		run(source, resultFormat, result);
	}

	@Override
	public String getName() {
		return "invert-mappings";
	}

	@Override
	public String getDescription() {
		return "Flips the source names with the destination names, ie. 'class a -> Example' becomes 'class Example -> a'.";
	}

	public static void run(Path sourceFile, String resultFormat, Path resultFile) throws MappingParseException, IOException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		MappingFormat format = MappingFormat.parseFromFile(sourceFile);

		EntryTree<EntryMapping> source = format.read(sourceFile);
		EntryTree<EntryMapping> result = MappingOperations.invert(source);

		Utils.delete(resultFile);
		MappingsWriter writer = MappingCommandsUtil.getWriter(resultFormat);
		writer.write(result, resultFile, saveParameters);
	}
}

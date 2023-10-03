package org.quiltmc.enigma.command;

import org.quiltmc.enigma.ProgressListener;
import org.quiltmc.enigma.translation.mapping.serde.MappingFormat;
import org.quiltmc.enigma.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

public class ConvertMappingsCommand extends Command {
	public ConvertMappingsCommand() {
		super(Argument.INPUT_MAPPINGS.required(),
				Argument.OUTPUT_MAPPING_FORMAT.required(),
				Argument.MAPPING_OUTPUT.required());
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
		return "convert-mappings";
	}

	@Override
	public String getDescription() {
		return "Converts the provided mappings to a different format.";
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

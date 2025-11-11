package org.quiltmc.enigma.command;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.command.InvertMappingsCommand.Optional;
import org.quiltmc.enigma.command.InvertMappingsCommand.Required;
import org.quiltmc.enigma.util.MappingOperations;
import org.quiltmc.enigma.util.Utils;

import java.io.IOException;
import java.nio.file.Path;

import static org.quiltmc.enigma.command.CommonArguments.DEOBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.MAPPING_OUTPUT;
import static org.quiltmc.enigma.command.CommonArguments.OBFUSCATED_NAMESPACE;

public final class InvertMappingsCommand extends Command<Required, Optional> {
	public static final InvertMappingsCommand INSTANCE = new InvertMappingsCommand();

	private InvertMappingsCommand() {
		super(
				ArgsParser.of(INPUT_MAPPINGS, MAPPING_OUTPUT, Required::new),
				ArgsParser.of(OBFUSCATED_NAMESPACE, DEOBFUSCATED_NAMESPACE, Optional::new)
		);
	}

	@Override
	void runImpl(Required required, Optional optional) throws IOException, MappingParseException {
		run(
				required.inputMappings, required.mappingOutput,
				optional.obfuscatedNamespace, optional.deobfuscatedNamespace
		);
	}

	@Override
	public String getName() {
		return "invert-mappings";
	}

	@Override
	public String getDescription() {
		return "Flips the source names with the destination names, ie. 'class a -> Example' becomes 'class Example -> a'.";
	}

	public static void run(Path sourceFile, Path resultFile, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws MappingParseException, IOException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		Enigma enigma = createEnigma();

		var readService = CommandsUtil.getReader(enigma, sourceFile);
		var writeService = CommandsUtil.getWriter(enigma, resultFile);

		EntryTree<EntryMapping> source = readService.read(sourceFile);
		EntryTree<EntryMapping> result = MappingOperations.invert(source);

		Utils.delete(resultFile);
		writeService.write(result, resultFile, saveParameters);
	}

	record Required(Path inputMappings, Path mappingOutput) { }

	record Optional(String obfuscatedNamespace, String deobfuscatedNamespace) { }
}

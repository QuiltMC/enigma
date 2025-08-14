package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.util.MappingOperations;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.quiltmc.enigma.command.CommonArguments.DEOBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.MAPPING_OUTPUT;
import static org.quiltmc.enigma.command.CommonArguments.OBFUSCATED_NAMESPACE;

public final class InvertMappingsCommand extends Command {
	public static final InvertMappingsCommand INSTANCE = new InvertMappingsCommand();

	private InvertMappingsCommand() {
		super(
				ImmutableList.of(INPUT_MAPPINGS, MAPPING_OUTPUT),
				ImmutableList.of(OBFUSCATED_NAMESPACE, DEOBFUSCATED_NAMESPACE)
		);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws IOException, MappingParseException {
		run(
				INPUT_MAPPINGS.get(args),
				MAPPING_OUTPUT.get(args),
				OBFUSCATED_NAMESPACE.get(args),
				DEOBFUSCATED_NAMESPACE.get(args)
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
}

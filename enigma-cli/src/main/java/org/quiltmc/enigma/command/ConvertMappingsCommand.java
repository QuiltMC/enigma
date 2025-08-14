package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
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

public final class ConvertMappingsCommand extends Command {
	public static final ConvertMappingsCommand INSTANCE = new ConvertMappingsCommand();

	private ConvertMappingsCommand() {
		super(INPUT_MAPPINGS, MAPPING_OUTPUT, OBFUSCATED_NAMESPACE, DEOBFUSCATED_NAMESPACE);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws IOException, MappingParseException {
		run(
				getReadablePath(args.get(INPUT_MAPPINGS.getName())),
				getWritablePath(args.get(MAPPING_OUTPUT.getName())),
				args.get(OBFUSCATED_NAMESPACE.getName()),
				args.get(DEOBFUSCATED_NAMESPACE.getName())
		);
	}

	@Override
	public String getName() {
		return "convert-mappings";
	}

	@Override
	public String getDescription() {
		return "Converts the provided mappings to a different format.";
	}

	public static void run(Path source, Path output, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws MappingParseException, IOException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		Enigma enigma = createEnigma();

		MappingsReader reader = CommandsUtil.getReader(enigma, source);
		EntryTree<EntryMapping> mappings = reader.read(source);

		Utils.delete(output);
		MappingsWriter writer = CommandsUtil.getWriter(enigma, output);
		writer.write(mappings, output, ProgressListener.createEmpty(), saveParameters);
	}
}

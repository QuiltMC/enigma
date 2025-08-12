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

public final class ConvertMappingsCommand extends Command {
	public static final ConvertMappingsCommand INSTANCE = new ConvertMappingsCommand();

	private ConvertMappingsCommand() {
		super(
				CommonArguments.INPUT_MAPPINGS,
				CommonArguments.MAPPING_OUTPUT,
				CommonArguments.OBFUSCATED_NAMESPACE,
				CommonArguments.DEOBFUSCATED_NAMESPACE
		);
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path source = getReadablePath(this.getArg(args, 0));
		Path result = getWritablePath(this.getArg(args, 1));
		String obfuscatedNamespace = this.getArg(args, 2);
		String deobfuscatedNamespace = this.getArg(args, 3);

		run(source, result, obfuscatedNamespace, deobfuscatedNamespace);
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

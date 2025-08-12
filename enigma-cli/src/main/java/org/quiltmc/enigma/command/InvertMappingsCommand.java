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

public class InvertMappingsCommand extends Command {
	private static final Argument OUTPUT_FOLDER = new Argument("<output-folder>",
			"""
					A path to the file or folder to write output to."""
	);

	public InvertMappingsCommand() {
		super(
				ImmutableList.of(CommonArguments.INPUT_MAPPINGS, OUTPUT_FOLDER),
				ImmutableList.of(CommonArguments.OBFUSCATED_NAMESPACE, CommonArguments.DEOBFUSCATED_NAMESPACE)
		);
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path source = getReadablePath(this.getArg(args, 0));
		Path result = getWritablePath(this.getArg(args, 2));
		String obfuscatedNamespace = this.getArg(args, 3);
		String deobfuscatedNamespace = this.getArg(args, 4);

		run(source, result, obfuscatedNamespace, deobfuscatedNamespace);
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

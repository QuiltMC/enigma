package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.util.MappingOperations;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public class ComposeMappingsCommand extends Command {
	public ComposeMappingsCommand() {
		super(Argument.LEFT_MAPPINGS.required(),
				Argument.RIGHT_MAPPINGS.required(),
				Argument.MAPPING_OUTPUT.required(),
				Argument.KEEP_MODE.required()
		);
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path left = getReadablePath(this.getArg(args, 0));
		Path right = getReadablePath(this.getArg(args, 1));
		Path result = getWritablePath(this.getArg(args, 2));
		String keepMode = this.getArg(args, 3);
		String obfuscatedNamespace = this.getArg(args, 4);
		String deobfuscatedNamespace = this.getArg(args, 5);

		run(left, right, result, keepMode, obfuscatedNamespace, deobfuscatedNamespace);
	}

	@Override
	public String getName() {
		return "compose-mappings";
	}

	@Override
	public String getDescription() {
		return "Merges the two mapping trees (left and right) into a common (middle) name set, handling conflicts according to the given \"keep mode\".";
	}

	public static void run(Path leftFile, Path rightFile, Path resultFile, String keepMode, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		Enigma enigma = createEnigma();

		MappingsReader leftReader = CommandsUtil.getReader(enigma, leftFile);
		EntryTree<EntryMapping> left = leftReader.read(leftFile);
		MappingsReader rightReader = CommandsUtil.getReader(enigma, rightFile);
		EntryTree<EntryMapping> right = rightReader.read(rightFile);
		EntryTree<EntryMapping> result = MappingOperations.compose(left, right, keepMode.equals("left") || keepMode.equals("both"), keepMode.equals("right") || keepMode.equals("both"));

		MappingsWriter writer = CommandsUtil.getWriter(enigma, resultFile);
		Utils.delete(resultFile);
		writer.write(result, resultFile, ProgressListener.createEmpty(), saveParameters);
	}
}

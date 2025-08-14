package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
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
import java.util.Map;

import static org.quiltmc.enigma.command.CommonArguments.DEOBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.command.CommonArguments.MAPPING_OUTPUT;
import static org.quiltmc.enigma.command.CommonArguments.OBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.util.Utils.andJoin;

public final class ComposeMappingsCommand extends Command {
	private static final Argument<Path> LEFT_MAPPINGS = Argument.ofReadablePath("left-mappings",
			"""
					A path to the left file or folder to read mappings from, used in commands which take two mapping inputs."""
	);
	private static final Argument<Path> RIGHT_MAPPINGS = Argument.ofReadablePath("right-mappings",
			"""
					A path to the right file or folder to read mappings from, used in commands which take two mapping inputs."""
	);
	private static final Argument<String> KEEP_MODE = Argument.ofLenientEnum("keep-mode", KeepMode.class,
			"""
					Which mappings should overwrite the others when composing conflicting mappings. Allowed values are """
				+ andJoin(KeepMode.VALUES.stream().map(Object::toString).map(mode -> '"' + mode + '"').toList())
	);

	public static final ComposeMappingsCommand INSTANCE = new ComposeMappingsCommand();

	private ComposeMappingsCommand() {
		super(
				ImmutableList.of(LEFT_MAPPINGS, RIGHT_MAPPINGS, MAPPING_OUTPUT, KEEP_MODE),
				ImmutableList.of(OBFUSCATED_NAMESPACE, DEOBFUSCATED_NAMESPACE)
		);
	}

	@Override
	protected void runImpl(Map<String, String> args) throws IOException, MappingParseException {
		run(
				LEFT_MAPPINGS.get(args), RIGHT_MAPPINGS.get(args),
				MAPPING_OUTPUT.get(args), KEEP_MODE.get(args),
				OBFUSCATED_NAMESPACE.get(args), DEOBFUSCATED_NAMESPACE.get(args)
		);
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
		final boolean keepLeft;
		final boolean keepRight;
		switch (keepMode) {
			case "left" -> {
				keepLeft = true;
				keepRight = false;
			}
			case "right" -> {
				keepLeft = false;
				keepRight = true;
			}
			case "both" -> {
				keepLeft = true;
				keepRight = true;
			}
			default -> {
				keepLeft = false;
				keepRight = false;
			}
		}

		run(leftFile, rightFile, resultFile, keepLeft, keepRight, obfuscatedNamespace, deobfuscatedNamespace);
	}

	public static void run(Path leftFile, Path rightFile, Path resultFile, KeepMode keepMode, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws IOException, MappingParseException {
		run(leftFile, rightFile, resultFile, keepMode.left, keepMode.right, obfuscatedNamespace, deobfuscatedNamespace);
	}

	private static void run(Path leftFile, Path rightFile, Path resultFile, boolean keepLeft, boolean keepRight, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		Enigma enigma = createEnigma();

		MappingsReader leftReader = CommandsUtil.getReader(enigma, leftFile);
		EntryTree<EntryMapping> left = leftReader.read(leftFile);
		MappingsReader rightReader = CommandsUtil.getReader(enigma, rightFile);
		EntryTree<EntryMapping> right = rightReader.read(rightFile);
		EntryTree<EntryMapping> result = MappingOperations.compose(left, right, keepLeft, keepRight);

		MappingsWriter writer = CommandsUtil.getWriter(enigma, resultFile);
		Utils.delete(resultFile);
		writer.write(result, resultFile, ProgressListener.createEmpty(), saveParameters);
	}

	public enum KeepMode {
		LEFT(true, false), RIGHT(false, true), BOTH(true, true);

		public static final ImmutableList<KeepMode> VALUES = ImmutableList.copyOf(values());

		private final boolean left;
		private final boolean right;

		private final String value;

		KeepMode(boolean left, boolean right) {
			this.left = left;
			this.right = right;

			this.value = this.name().toLowerCase();
		}

		@Override
		public String toString() {
			return this.value;
		}
	}
}

package cuchaz.enigma.command;

import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MapSpecializedMethodsCommand extends Command {
	private static final String NAME = "map-specialized-methods";

	public MapSpecializedMethodsCommand() {
		super(NAME);
	}

	@Override
	public String getUsage() {
		return "<jar> <source-format> <source> <result-format> <result>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 5;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path jar = getReadablePath(getArg(args, 0, "jar", true));
		String sourceFormat = getArg(args, 1, "source-format", true);
		Path source = getReadablePath(getArg(args, 2, "source", true));
		String resultFormat = getArg(args, 3, "result-format", true);
		Path result = getWritablePath(getArg(args, 4, "result", true));

		run(jar, sourceFormat, source, resultFormat, result);
	}

	public static void run(Path jar, String sourceFormat, Path sourcePath, String resultFormat, Path output) throws IOException, MappingParseException {
		boolean debug = shouldDebug(NAME);
		JarIndex jarIndex = loadJar(jar);

		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> source = MappingCommandsUtil.read(sourceFormat, sourcePath, saveParameters);

		EntryTree<EntryMapping> result = run(jarIndex, source, debug);

		Utils.delete(output);
		MappingCommandsUtil.write(result, resultFormat, output, saveParameters);

		if (debug) {
			writeDebugDelta((DeltaTrackingTree<EntryMapping>) result, output);
		}
	}

	public static EntryTree<EntryMapping> run(JarIndex jarIndex, EntryTree<EntryMapping> source, boolean trackDelta) throws IOException, MappingParseException {
		EntryTree<EntryMapping> result = new HashEntryTree<>();

		BridgeMethodIndex bridgeMethodIndex = jarIndex.getBridgeMethodIndex();
		Translator translator = new MappingTranslator(source, jarIndex.getEntryResolver());

		// Copy all non-specialized methods
		for (EntryTreeNode<EntryMapping> node : source) {
			if (!(node.getEntry() instanceof MethodEntry) || !bridgeMethodIndex.isSpecializedMethod((MethodEntry) node.getEntry())) {
				result.insert(node.getEntry(), node.getValue());
			}
		}

		if (trackDelta) {
			result = new DeltaTrackingTree<>(result);
		}

		// Add correct mappings for specialized methods
		for (Map.Entry<MethodEntry, MethodEntry> entry : bridgeMethodIndex.getBridgeToSpecialized().entrySet()) {
			MethodEntry bridge = entry.getKey();
			MethodEntry specialized = entry.getValue();
			String name = translator.translate(bridge).getName();
			result.insert(specialized, new EntryMapping(name));
		}

		return result;
	}
}

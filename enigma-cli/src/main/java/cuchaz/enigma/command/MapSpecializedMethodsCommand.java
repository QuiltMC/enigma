package cuchaz.enigma.command;

import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
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
	public MapSpecializedMethodsCommand() {
		super(Argument.INPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.required(),
				Argument.OUTPUT_MAPPING_FORMAT.required(),
				Argument.MAPPING_OUTPUT.required());
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path jar = getReadablePath(this.getArg(args, 0));
		Path source = getReadablePath(this.getArg(args, 1));
		String resultFormat = this.getArg(args, 2);
		Path result = getWritablePath(this.getArg(args, 3));

		run(jar, source, resultFormat, result);
	}

	@Override
	public String getName() {
		return "map-specialized-methods";
	}

	@Override
	public String getDescription() {
		return "Adds names for specialized methods from their corresponding bridge method";
	}

	public static void run(Path jar, Path sourcePath, String resultFormat, Path output) throws IOException, MappingParseException {
		boolean debug = shouldDebug(new MapSpecializedMethodsCommand().getName());
		JarIndex jarIndex = loadJar(jar);

		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		MappingFormat sourceFormat = MappingFormat.parseFromFile(sourcePath);
		EntryTree<EntryMapping> source = sourceFormat.read(sourcePath);

		EntryTree<EntryMapping> result = run(jarIndex, source, debug);

		Utils.delete(output);
		MappingsWriter writer = MappingCommandsUtil.getWriter(resultFormat);
		writer.write(result, output, saveParameters);

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

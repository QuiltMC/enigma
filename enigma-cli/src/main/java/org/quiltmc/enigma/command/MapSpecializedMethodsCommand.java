package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.MappingTranslator;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class MapSpecializedMethodsCommand extends Command {
	public MapSpecializedMethodsCommand() {
		super(
				ImmutableList.of(CommonArguments.INPUT_JAR, CommonArguments.INPUT_MAPPINGS, CommonArguments.MAPPING_OUTPUT),
				ImmutableList.of(CommonArguments.OBFUSCATED_NAMESPACE, CommonArguments.DEOBFUSCATED_NAMESPACE)
		);
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		Path jar = getReadablePath(this.getArg(args, 0));
		Path source = getReadablePath(this.getArg(args, 1));
		Path result = getWritablePath(this.getArg(args, 2));
		String obfuscatedNamespace = this.getArg(args, 3);
		String deobfuscatedNamespace = this.getArg(args, 4);

		run(jar, source, result, obfuscatedNamespace, deobfuscatedNamespace);
	}

	@Override
	public String getName() {
		return "map-specialized-methods";
	}

	@Override
	public String getDescription() {
		return "Adds names for specialized methods from their corresponding bridge method";
	}

	public static void run(Path jar, Path sourcePath, Path output, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws IOException, MappingParseException {
		boolean debug = shouldDebug(new MapSpecializedMethodsCommand().getName());
		JarIndex jarIndex = loadJar(jar);
		Enigma enigma = createEnigma();

		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		MappingsReader reader = CommandsUtil.getReader(enigma, sourcePath);
		EntryTree<EntryMapping> source = reader.read(sourcePath);

		EntryTree<EntryMapping> result = run(jarIndex, source, debug);

		Utils.delete(output);
		MappingsWriter writer = CommandsUtil.getWriter(enigma, output);
		writer.write(result, output, saveParameters);

		if (debug) {
			writeDebugDelta((DeltaTrackingTree<EntryMapping>) result, output);
		}
	}

	public static EntryTree<EntryMapping> run(JarIndex jarIndex, EntryTree<EntryMapping> source, boolean trackDelta) throws IOException, MappingParseException {
		EntryTree<EntryMapping> result = new HashEntryTree<>();

		BridgeMethodIndex bridgeMethodIndex = jarIndex.getIndex(BridgeMethodIndex.class);
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

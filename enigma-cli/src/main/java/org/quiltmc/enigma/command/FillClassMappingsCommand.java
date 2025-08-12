package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.util.Utils;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

public final class FillClassMappingsCommand extends Command {
	private static final Argument FILL_ALL = new Argument("<fill-all>",
			"""
					Whether to fill all possible mappings. Allowed values are "true" and "false"."""
	);

	public static final FillClassMappingsCommand INSTANCE = new FillClassMappingsCommand();

	private FillClassMappingsCommand() {
		super(
				ImmutableList.of(
						CommonArguments.INPUT_JAR,
						CommonArguments.INPUT_MAPPINGS,
						CommonArguments.MAPPING_OUTPUT
				),
				ImmutableList.of(FILL_ALL, CommonArguments.OBFUSCATED_NAMESPACE, CommonArguments.DEOBFUSCATED_NAMESPACE)
		);
	}

	@Override
	public void run(String... args) throws Exception {
		Path inJar = getReadablePath(this.getArg(args, 0));
		Path source = getReadablePath(this.getArg(args, 1));
		Path result = getWritablePath(this.getArg(args, 2));
		boolean fillAll = Boolean.parseBoolean(this.getArg(args, 3));
		String obfuscatedNamespace = this.getArg(args, 4);
		String deobfuscatedNamespace = this.getArg(args, 5);

		run(inJar, source, result, fillAll, obfuscatedNamespace, deobfuscatedNamespace);
	}

	@Override
	public String getName() {
		return "fill-class-mappings";
	}

	@Override
	public String getDescription() {
		return "Adds empty mappings for classes missing in the input file, but whose parent or ancestors, do have names";
	}

	public static void run(Path jar, Path source, Path result, boolean fillAll, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws Exception {
		boolean debug = shouldDebug(INSTANCE.getName());
		JarIndex jarIndex = loadJar(jar);
		Enigma enigma = createEnigma();

		Logger.info("Reading mappings...");
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, obfuscatedNamespace, deobfuscatedNamespace);
		EntryTree<EntryMapping> sourceMappings = readMappings(enigma, source, ProgressListener.createEmpty());

		EntryTree<EntryMapping> resultMappings = exec(jarIndex, sourceMappings, fillAll, debug);

		Logger.info("Writing mappings...");
		MappingsWriter writer = CommandsUtil.getWriter(enigma, result);
		Utils.delete(result);
		writer.write(resultMappings, result, ProgressListener.createEmpty(), saveParameters);

		if (debug) {
			writeDebugDelta((DeltaTrackingTree<EntryMapping>) resultMappings, result);
		}
	}

	/**
	 * Fill class mappings for the given source tree.
	 *
	 * @param jarIndex the jar index to analyze which classes should get a name
	 * @param source the mapping tree to fill in
	 * @param fillAll add mappings for all existing classes.
	 *                   This option adds mappings for a whole subtree if its root node, the top-level class node, exists.
	 *                   That is, if any of its children (or their children and so on) have a mapping
	 * @param trackDelta whether to use a {@link DeltaTrackingTree} for the result
	 * @return the resulting mapping tree
	 */
	public static EntryTree<EntryMapping> exec(JarIndex jarIndex, EntryTree<EntryMapping> source, boolean fillAll, boolean trackDelta) {
		EntryTree<EntryMapping> result = new HashEntryTree<>(source);

		if (trackDelta) {
			result = new DeltaTrackingTree<>(result);
		}

		Logger.info("Adding mappings...");
		List<ClassEntry> rootEntries = source.getRootNodes().map(EntryTreeNode::getEntry)
				.filter(entry -> entry instanceof ClassEntry)
				.map(entry -> (ClassEntry) entry)
				.toList();
		for (ClassEntry rootEntry : rootEntries) {
			// These entries already have a mapping tree node
			recursiveAddMappings(result, jarIndex, rootEntry, fillAll);
		}

		return result;
	}

	private static void recursiveAddMappings(EntryTree<EntryMapping> mappings, JarIndex index, ClassEntry entry, boolean addMapping) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		boolean hasMapping = node != null && node.hasValue() && node.getValue().targetName() != null;

		Logger.debug("Entry {} {} a mapping", entry, hasMapping ? "has" : "doesn't have");
		if (!hasMapping && addMapping) {
			Logger.debug("Adding mapping for {}", entry);
			mappings.insert(entry, EntryMapping.OBFUSCATED);
		}

		List<ParentedEntry<?>> children = index.getChildrenByClass().get(entry);
		for (ParentedEntry<?> child : children) {
			if (child instanceof ClassEntry classChild) {
				recursiveAddMappings(mappings, index, classChild, addMapping || hasMapping);
			}
		}
	}
}

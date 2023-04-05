package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.Utils;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.List;

public class FillClassMappingsCommand extends Command {
	public static final String NAME = "fill-class-mappings";

	protected FillClassMappingsCommand() {
		super(NAME);
	}

	@Override
	public String getUsage() {
		return "<in-jar> <source> <result> <result-format> [<fill-all>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 4 || length == 5;
	}

	@Override
	public void run(String... args) throws Exception {
		Path inJar = getReadablePath(getArg(args, 0, "in-jar", true));
		Path source = getReadablePath(getArg(args, 1, "source", true));
		Path result = getWritablePath(getArg(args, 2, "result", true));
		String resultFormat = getArg(args, 3, "result-format", true);
		boolean fillAll = Boolean.parseBoolean(getArg(args, 4, "fill-all", false));

		run(inJar, source, result, resultFormat, fillAll);
	}

	public static void run(Path jar, Path source, Path result, String resultFormat, boolean fillAll) throws Exception {
		boolean debug = shouldDebug(NAME);
		JarIndex jarIndex = loadJar(jar);

		Logger.info("Reading mappings...");
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> sourceMappings = readMappings(source, ProgressListener.none(), saveParameters);

		EntryTree<EntryMapping> resultMappings = exec(jarIndex, sourceMappings, fillAll, debug);

		Logger.info("Writing mappings...");
		Utils.delete(result);
		MappingCommandsUtil.write(resultMappings, resultFormat, result, saveParameters);

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
			mappings.insert(entry, EntryMapping.DEFAULT);
		}

		List<ParentedEntry<?>> children = index.getChildrenByClass().get(entry);
		for (ParentedEntry<?> child : children) {
			if (child instanceof ClassEntry classChild) {
				recursiveAddMappings(mappings, index, classChild, addMapping || hasMapping);
			}
		}
	}
}

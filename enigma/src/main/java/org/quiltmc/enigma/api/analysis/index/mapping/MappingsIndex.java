package org.quiltmc.enigma.api.analysis.index.mapping;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.I18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A consolidated {@link MappingsIndexer} that can be configured to use as many separate indexers as you like.
 */
public class MappingsIndex implements MappingsIndexer {
	private final Map<Class<? extends MappingsIndexer>, MappingsIndexer> indexers = new LinkedHashMap<>();

	private ProgressListener progress;
	private int work;

	/**
	 * Creates a new empty index with all provided indexers.
	 * Indexers will be run in the order they're passed to this constructor.
	 * @param indexers the indexers to use
	 */
	public MappingsIndex(MappingsIndexer... indexers) {
		for (MappingsIndexer indexer : indexers) {
			this.indexers.put(indexer.getClass(), indexer);
		}
	}

	/**
	 * Creates an empty index, configured to use all built-in indexers.
	 * @return the newly created index
	 */
	public static MappingsIndex empty() {
		return new MappingsIndex(new PackageIndex());
	}

	/**
	 * Gets the index associated with the provided class.
	 * @param clazz the class of the index desired - for example, {@code PackageIndex.class}
	 * @return the index
	 */
	@SuppressWarnings("unchecked")
	public <T extends MappingsIndexer> T getIndex(Class<T> clazz) {
		MappingsIndexer index = this.indexers.get(clazz);
		if (index != null) {
			return (T) index;
		} else {
			throw new IllegalArgumentException("no indexer registered for class " + clazz);
		}
	}

	/**
	 * Runs every configured indexer over each mapping in the tree.
	 * @param mappings the mappings to index
	 * @param progress a progress listener to track index completion
	 */
	public void indexMappings(EntryTree<EntryMapping> mappings, ProgressListener progress) {
		this.progress = progress;

		Collection<EntryTreeNode<EntryMapping>> nodes = new ArrayList<>();
		for (EntryTreeNode<EntryMapping> node : mappings.getRootNodes().toList()) {
			nodes.addAll(node.getNodesRecursively());
		}

		this.indexNodes(nodes, true);
	}

	public void reindexClass(ClassEntry classEntry, EntryTree<EntryMapping> oldMappings, EntryTree<EntryMapping> newMappings, ProgressListener progress) {
		this.progress = progress;
		oldMappings.remove(classEntry);

		EntryTreeNode<EntryMapping> classNode = newMappings.findNode(classEntry);
		if (classNode == null) {
			throw new IllegalArgumentException("Class entry not found in mappings");
		}

		oldMappings.insert(classNode);
		this.indexNodes(classNode.getNodesRecursively(), false);
	}

	private void indexNodes(Collection<? extends EntryTreeNode<EntryMapping>> nodes, boolean postProcess) {
		this.work = nodes.isEmpty() ? 1 : nodes.size();
		this.progress.init(this.work, I18n.translate("progress.mappings.indexing.mappings"));

		for (var node : nodes) {
			Entry<?> entry = node.getEntry();
			EntryMapping mapping = node.getValue();

			if (mapping != null) {
				if (entry instanceof ClassEntry classEntry) {
					this.indexClassMapping(mapping, classEntry);
				} else if (entry instanceof MethodEntry methodEntry) {
					this.indexMethodMapping(mapping, methodEntry);
				} else if (entry instanceof FieldEntry fieldEntry) {
					this.indexFieldMapping(mapping, fieldEntry);
				} else if (entry instanceof LocalVariableEntry localVariableEntry) {
					this.indexLocalVariableMapping(mapping, localVariableEntry);
				}
			}

			this.progress.step(this.work++, I18n.translate("progress.mappings.indexing.mappings"));
		}

		if (postProcess) {
			this.processIndex(this);
		}

		this.progress = null;
		this.work = 0;
	}

	public void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
		this.indexers.forEach((key, indexer) -> indexer.indexClassMapping(mapping, entry));
	}

	public void indexMethodMapping(EntryMapping mapping, MethodEntry entry) {
		this.indexers.forEach((key, indexer) -> indexer.indexMethodMapping(mapping, entry));
	}

	public void indexFieldMapping(EntryMapping mapping, FieldEntry entry) {
		this.indexers.forEach((key, indexer) -> indexer.indexFieldMapping(mapping, entry));
	}

	public void indexLocalVariableMapping(EntryMapping mapping, LocalVariableEntry entry) {
		this.indexers.forEach((key, indexer) -> indexer.indexLocalVariableMapping(mapping, entry));
	}

	@Override
	public void processIndex(MappingsIndex index) {
		this.stepProcessingProgress("progress.mappings.indexing.process.mappings");

		this.indexers.forEach((key, indexer) -> {
			this.stepProcessingProgress(indexer.getTranslationKey());
			indexer.processIndex(index);
		});

		this.stepProcessingProgress("progress.mappings.indexing.process.done");
	}

	@Override
	public void reindexEntry(EntryMapping newMapping, Entry<?> entry) {
		this.indexers.forEach((key, indexer) -> indexer.reindexEntry(newMapping, entry));
	}

	private void stepProcessingProgress(String key) {
		if (this.progress != null) {
			this.progress.step(this.work, I18n.translateFormatted("progress.mappings.indexing.process", I18n.translate(key)));
		}
	}

	@Override
	public String getTranslationKey() {
		return "progress.mappings.indexing.mappings";
	}
}

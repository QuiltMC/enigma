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
import org.quiltmc.enigma.util.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MappingsIndex implements MappingsIndexer {
	private final Collection<MappingsIndexer> indexers;
	private final PackageIndex packageIndex;

	private ProgressListener progress;
	private int work;

	public MappingsIndex(PackageIndex packageIndex) {
		this.packageIndex = packageIndex;
		this.indexers = List.of(this.packageIndex);
	}

	public static MappingsIndex empty() {
		return new MappingsIndex(new PackageIndex());
	}

	public PackageIndex getPackageIndex() {
		return this.packageIndex;
	}

	public void indexMappings(EntryTree<EntryMapping> mappings, ProgressListener progress) {
		this.progress = progress;

		Set<Pair<Entry<?>, EntryMapping>> entries = new HashSet<>();

		mappings.getRootNodes().forEach(node -> handleNode(node, entries));

		this.work = entries.size();
		this.progress.init(this.work, I18n.translate("progress.mappings.indexing"));

		for (var pair : entries) {
			Entry<?> entry = pair.a();
			EntryMapping mapping = pair.b();

			if (entry instanceof ClassEntry classEntry) {
				this.indexClassMapping(mapping, classEntry);
			} else if (entry instanceof MethodEntry methodEntry) {
				this.indexMethodMapping(mapping, methodEntry);
			} else if (entry instanceof FieldEntry fieldEntry) {
				this.indexFieldMapping(mapping, fieldEntry);
			} else if (entry instanceof LocalVariableEntry localVariableEntry) {
				this.indexLocalVariableMapping(mapping, localVariableEntry);
			}

			this.progress.step(this.work++, I18n.translate("progress.mappings.indexing"));
		}

		this.processIndex(this);

		this.progress = null;
		this.work = 0;
	}

	private static void handleNode(EntryTreeNode<EntryMapping> node, Set<Pair<Entry<?>, EntryMapping>> entries) {
		if (!node.getChildNodes().isEmpty()) {
			node.getChildNodes().forEach(child -> handleNode(child, entries));
		} else {
			entries.add(new Pair<>(node.getEntry(), node.getValue()));
		}
	}

	@Override
	public void processIndex(MappingsIndex index) {
		this.stepProcessingProgress("progress.mappings.indexing.process.mappings");

		this.indexers.forEach(indexer -> {
			this.stepProcessingProgress(indexer.getTranslationKey());
			indexer.processIndex(index);
		});

		this.stepProcessingProgress("progress.mappings.indexing.process.done");
	}

	@Override
	public void reindexEntry(EntryMapping newMapping, Entry<?> entry) {
		this.indexers.forEach(indexer -> indexer.reindexEntry(newMapping, entry));
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

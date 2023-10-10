package org.quiltmc.enigma.api.analysis.index.mapping;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageIndex implements MappingsIndexer {
	private final Map<ClassEntry, String> packageNames = new HashMap<>();

	@Override
	public void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
		if (mapping.targetName() == null) {
			return;
		}

		String packageName = ClassEntry.getParentPackage(mapping.targetName());
		if (!entry.isInnerClass() && !this.packageNames.containsValue(packageName)) {
			this.packageNames.put(entry, packageName);
		}
	}

	@Override
	public void reindexEntry(EntryMapping newMapping, Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			this.packageNames.remove(classEntry);
			this.indexClassMapping(newMapping, classEntry);
		}
	}

	public List<String> getPackageNames() {
		return this.packageNames.values().stream().distinct().toList();
	}

	@Override
	public String getTranslationKey() {
		return "progress.mappings.indexing.packages";
	}
}

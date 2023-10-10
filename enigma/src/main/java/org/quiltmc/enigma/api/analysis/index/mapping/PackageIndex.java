package org.quiltmc.enigma.api.analysis.index.mapping;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.util.ArrayList;
import java.util.List;

public class PackageIndex implements MappingsIndexer {
	private final List<String> packageNames = new ArrayList<>();

	@Override
	public void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
		if (!entry.isInnerClass() && !this.packageNames.contains(entry.getPackageName())) {
			this.packageNames.add(entry.getPackageName());
		}
	}

	public List<String> getPackageNames() {
		return this.packageNames;
	}

	@Override
	public String getTranslationKey() {
		return "progress.mappings.indexing.packages";
	}
}

package org.quiltmc.enigma.api.analysis.index.mapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.List;

/**
 * An indexer that saves the names of all currently existing packages.
 */
public class PackageIndex implements MappingsIndexer {
	private final Multimap<ClassEntry, String> packageNames = HashMultimap.create();

	@Override
	public void indexClassMapping(EntryMapping mapping, ClassEntry entry) {
		// only index for top-level classes
		if (entry.isInnerClass()) {
			return;
		}

		String name = mapping.targetName();
		if (name == null) {
			name = entry.getFullName();
		}

		if (name != null) {
			// index all different versions of the package name
			// note we're not avoiding duplicates: otherwise reindexing would erroneously remove package names
			while (name != null && name.contains("/")) {
				name = ClassEntry.getParentPackage(name);
				this.packageNames.put(entry, name);
			}
		}
	}

	@Override
	public void reindexEntry(EntryMapping newMapping, Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			this.packageNames.removeAll(classEntry);
			this.indexClassMapping(newMapping, classEntry);
		}
	}

	/**
	 * Gets all distinct package names.
	 * @return a list of unique package names
	 */
	public List<String> getPackageNames() {
		return this.packageNames.values().stream().distinct().toList();
	}

	@Override
	public String getTranslationKey() {
		return "progress.mappings.indexing.packages";
	}
}

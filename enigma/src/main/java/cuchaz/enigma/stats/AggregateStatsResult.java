package cuchaz.enigma.stats;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateStatsResult implements StatsResult {
	private final EnigmaProject project;

	private final Map<String, List<ClassStatsResult>> packageToClasses = new HashMap<>();
	private final Map<ClassEntry, ClassStatsResult> stats = new HashMap<>();
	private final Map<ClassEntry, String> classToPackage = new HashMap<>();

	private ClassStatsResult overall;

	public AggregateStatsResult(EnigmaProject project, Map<ClassEntry, ClassStatsResult> stats) {
		this.project = project;

		for (var entry : stats.entrySet()) {
			ClassEntry classEntry = entry.getKey();
			ClassStatsResult statEntry = entry.getValue();

			this.stats.put(classEntry, statEntry);
			this.updatePackage(classEntry, statEntry);
		}

		this.rebuildOverall(null);
	}

	public Map<ClassEntry, ClassStatsResult> getStats() {
		return this.stats;
	}

	public void updatePackage(ClassEntry obfEntry, ClassStatsResult newStats) {
		ClassEntry deobfuscated = this.project.getMapper().deobfuscate(obfEntry);
		ClassEntry classEntry = deobfuscated == null ? obfEntry : deobfuscated;

		String packageName = classEntry.getPackageName();
		String oldPackageName = this.classToPackage.get(obfEntry) == null ? packageName : this.classToPackage.get(obfEntry);

		this.packageToClasses.putIfAbsent(packageName, new ArrayList<>());
		this.classToPackage.remove(obfEntry);
		this.classToPackage.putIfAbsent(classEntry, packageName);

		// remove old result
		List<ClassStatsResult> oldPackageResults = this.packageToClasses.get(oldPackageName);

		ClassStatsResult oldResult = null;
		for (ClassStatsResult result : oldPackageResults) {
			if (result.obfEntry().equals(obfEntry)) {
				oldResult = result;
			}
		}

		if (oldResult != null) {
			oldPackageResults.remove(oldResult);
		}

		List<ClassStatsResult> newResults = this.packageToClasses.get(packageName);
		newResults.add(newStats);

		this.rebuildOverall(null);
	}

	private void rebuildOverall(@Nullable List<ClassEntry> updated) {
		if (updated == null) {
			Map<StatType, Integer> totalMappable = new HashMap<>();
			Map<StatType, Integer> totalUnmapped = new HashMap<>();

			for (var entry : this.stats.entrySet()) {
				ClassStatsResult result = entry.getValue();

				for (var unmappedEntry : result.totalUnmapped().entrySet()) {
					totalUnmapped.put(unmappedEntry.getKey(), totalUnmapped.getOrDefault(unmappedEntry.getKey(), 0) + unmappedEntry.getValue());
				}

				for (var mappableEntry : result.totalMappable().entrySet()) {
					totalMappable.put(mappableEntry.getKey(), totalMappable.getOrDefault(mappableEntry.getKey(), 0) + mappableEntry.getValue());
				}
			}

			this.overall = new ClassStatsResult(null, totalMappable, totalUnmapped);
		} else {
			for (ClassEntry entry : updated) {
				ClassStatsResult result = this.stats.get(entry);
				// todo
			}
		}
	}

	@Override
	public int getMappable(StatType... types) {
		return this.overall.getMappable(types);
	}

	@Override
	public int getUnmapped(StatType... types) {
		return this.overall.getUnmapped(types);
	}

	@Override
	public int getMapped(StatType... types) {
		return this.overall.getMapped(types);
	}
}

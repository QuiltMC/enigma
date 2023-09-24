package cuchaz.enigma.stats;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// todo doesn't handle package properly
public class ProjectStatsResult implements StatsProvider {
	private final EnigmaProject project;

	private final Map<String, List<StatsResult>> packageToClasses = new HashMap<>();
	private final Map<ClassEntry, StatsResult> stats = new HashMap<>();
	private final Map<ClassEntry, String> classToPackage = new HashMap<>();
	private final Map<String, StatsResult> packageStats = new HashMap<>();

	private StatsResult overall;

	public ProjectStatsResult(EnigmaProject project, Map<ClassEntry, StatsResult> stats) {
		this.project = project;

		for (var entry : stats.entrySet()) {
			ClassEntry classEntry = entry.getKey();
			StatsResult statEntry = entry.getValue();

			this.stats.put(classEntry, statEntry);
			this.updatePackage(classEntry, statEntry);
		}

		this.rebuildOverall();
	}

	public void updatePackage(ClassEntry obfEntry, StatsResult newStats) {
		ClassEntry deobfuscated = this.project.getMapper().deobfuscate(obfEntry);
		ClassEntry classEntry = deobfuscated == null ? obfEntry : deobfuscated;

		String packageName = classEntry.getPackageName() == null ? "" : classEntry.getPackageName();
		String oldPackageName = this.classToPackage.get(obfEntry) == null ? packageName : this.classToPackage.get(obfEntry);

		this.packageToClasses.putIfAbsent(packageName, new ArrayList<>());
		this.classToPackage.remove(obfEntry);
		this.classToPackage.put(obfEntry, packageName);

		// remove old result
		List<StatsResult> oldPackageResults = this.packageToClasses.get(oldPackageName);

		StatsResult oldResult = null;
		for (StatsResult result : oldPackageResults) {
			if (result.obfEntry() != null && result.obfEntry().equals(obfEntry)) {
				oldResult = result;
			}
		}

		if (oldResult != null) {
			oldPackageResults.remove(oldResult);
		}

		List<StatsResult> newResults = this.packageToClasses.get(packageName);
		newResults.add(newStats);

		this.rebuildOverall();
		this.rebuildPackageFor(obfEntry, oldPackageName);
	}

	private void rebuildOverall() {
		var maps = buildStats(this.stats.values());
		this.overall = new StatsResult(null, maps.a(), maps.b());
	}

	private static Pair<Map<StatType, Integer>, Map<StatType, Integer>> buildStats(Collection<StatsResult> stats) {
		Map<StatType, Integer> totalMappable = new HashMap<>();
		Map<StatType, Integer> totalUnmapped = new HashMap<>();

		for (StatsResult result : stats) {
			for (var unmappedEntry : result.totalUnmapped().entrySet()) {
				totalUnmapped.put(unmappedEntry.getKey(), totalUnmapped.getOrDefault(unmappedEntry.getKey(), 0) + unmappedEntry.getValue());
			}

			for (var mappableEntry : result.totalMappable().entrySet()) {
				totalMappable.put(mappableEntry.getKey(), totalMappable.getOrDefault(mappableEntry.getKey(), 0) + mappableEntry.getValue());
			}
		}

		return new Pair<>(totalMappable, totalUnmapped);
	}

	private void rebuildPackageFor(ClassEntry entry, String oldPackage) {
		String packageName = this.classToPackage.get(entry);

		if (!packageName.equals(oldPackage)) {
			var newStats = buildStats(this.packageToClasses.get(oldPackage));
			this.packageStats.put(oldPackage, new StatsResult(null, newStats.a(), newStats.b()));
		}

		var newStats = buildStats(this.packageToClasses.get(packageName));
		this.packageStats.put(packageName, new StatsResult(null, newStats.a(), newStats.b()));
	}

	public StatsResult getPackageStats(String name) {
		return this.packageStats.get(name);
	}

	public Map<ClassEntry, StatsResult> getStats() {
		return this.stats;
	}

	public StatsResult getOverall() {
		return this.overall;
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

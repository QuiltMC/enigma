package org.quiltmc.enigma.stats;

import com.strobel.core.Triple;
import org.quiltmc.enigma.EnigmaProject;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectStatsResult implements StatsProvider {
	private final EnigmaProject project;

	private final Map<String, List<StatsResult>> packageToClasses = new HashMap<>();
	private final Map<ClassEntry, StatsResult> stats = new HashMap<>();
	private final Map<String, StatsResult> packageStats = new HashMap<>();

	private StatsResult overall;

	/**
	 * Creates and indexes an overall result.
	 * @param project the project the stats are for
	 * @param stats a map of classes to results
	 */
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

	private void updatePackage(ClassEntry obfEntry, StatsResult newStats) {
		ClassEntry deobfuscated = this.project.getMapper().deobfuscate(obfEntry);
		ClassEntry classEntry = deobfuscated == null ? obfEntry : deobfuscated;

		String packageName = classEntry.getPackageName() == null ? "" : classEntry.getPackageName();
		List<String> packages = getPackages(packageName);

		this.addClass(packages, newStats);
		this.rebuildPackageFor(packages);
	}

	private void addClass(List<String> packages, StatsResult newStats) {
		for (String name : packages) {
			this.packageToClasses.putIfAbsent(name, new ArrayList<>());
			List<StatsResult> newResults = this.packageToClasses.get(name);
			newResults.add(newStats);
		}
	}

	private static List<String> getPackages(String packageName) {
		List<String> packages = new ArrayList<>();
		packages.add(packageName);
		for (int i = packageName.lastIndexOf('/'); i > 0; i--) {
			if (packageName.charAt(i) == '/') {
				packages.add(packageName.substring(0, i));
			}
		}

		return packages;
	}

	private void rebuildOverall() {
		var maps = this.buildStats(this.stats.values());
		this.overall = new StatsResult(maps.getFirst(), maps.getSecond(), maps.getThird(), false);
	}

	private Triple<Map<StatType, Integer>, Map<StatType, Integer>, Map<StatType, Map<String, Integer>>> buildStats(Collection<StatsResult> stats) {
		Map<StatType, Integer> totalMappable = new HashMap<>();
		Map<StatType, Integer> totalUnmapped = new HashMap<>();
		Map<StatType, Map<String, Integer>> unmappedTreeData = new HashMap<>();

		for (StatsResult result : stats) {
			for (var unmappedEntry : result.totalUnmapped().entrySet()) {
				totalUnmapped.put(unmappedEntry.getKey(), totalUnmapped.getOrDefault(unmappedEntry.getKey(), 0) + unmappedEntry.getValue());
			}

			for (var mappableEntry : result.totalMappable().entrySet()) {
				totalMappable.put(mappableEntry.getKey(), totalMappable.getOrDefault(mappableEntry.getKey(), 0) + mappableEntry.getValue());
			}

			if (!result.isPackage()) {
				for (var dataEntry : result.unmappedTreeData().entrySet()) {
					Map<String, Integer> classData = unmappedTreeData.getOrDefault(dataEntry.getKey(), new HashMap<>());

					for (var data : dataEntry.getValue().entrySet()) {
						classData.put(data.getKey(), classData.getOrDefault(data.getKey(), 0) + data.getValue());
					}

					unmappedTreeData.put(dataEntry.getKey(), classData);
				}
			}
		}

		return new Triple<>(totalMappable, totalUnmapped, unmappedTreeData);
	}

	private void rebuildPackageFor(List<String> packageNames) {
		for (String name : packageNames) {
			var newStats = this.buildStats(this.packageToClasses.get(name));
			this.packageStats.put(name, new StatsResult(newStats.getFirst(), newStats.getSecond(), newStats.getThird(), true));
		}
	}

	/**
	 * Filters results by the provided top-level package.
	 * @param topLevelPackage the package to get stats for, separated with slashes
	 * @return a new result, containing only classes which match the filter
	 */
	public ProjectStatsResult filter(String topLevelPackage) {
		Map<ClassEntry, StatsResult> newStats = new HashMap<>();

		for (var entry : this.stats.entrySet()) {
			ClassEntry deobfuscated = this.project.getMapper().deobfuscate(entry.getKey());
			ClassEntry classEntry = deobfuscated == null ? entry.getKey() : deobfuscated;

			String packageName = classEntry.getPackageName() == null ? "" : classEntry.getPackageName();
			if (packageName.startsWith(topLevelPackage)) {
				newStats.put(entry.getKey(), entry.getValue());
			}
		}

		return new ProjectStatsResult(this.project, newStats);
	}

	/**
	 * Gets the overall result for the provided package
	 * @param name the package name, separated by slashes
	 * @return the overall result
	 */
	public StatsResult getPackageStats(String name) {
		return this.packageStats.get(name);
	}

	/**
	 * Gets all per-class stats.
	 * @return the stats, as a class-to-result map
	 */
	public Map<ClassEntry, StatsResult> getStats() {
		return this.stats;
	}

	/**
	 * Gets the overall stats for the full project.
	 * @return the overall result
	 */
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

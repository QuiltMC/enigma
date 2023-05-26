package cuchaz.enigma.gui.stats;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.HashMap;

/**
 * A class to manage generated stats for class tree classes. This is used to avoid generating stats for a class multiple times.
 * <br>It simply consists of a map of class entries and stat results.
 */
public class StatsManager {
	private final HashMap<ClassEntry, StatsResult> results = new HashMap<>();

	/**
	 * Sets the stats for the given class entry.
	 * @param classEntry the entry
	 * @param stats the stats to associate
	 */
	public void setStats(ClassEntry classEntry, StatsResult stats) {
		this.results.put(classEntry, stats);
	}

	/**
	 * Gets the stats for the given class entry.
	 * @param classEntry the entry to get stats for
	 * @return the stats for the given class entry, or {@code null} if not yet generated
	 */
	public StatsResult getStats(ClassEntry classEntry) {
		return this.results.get(classEntry);
	}
}

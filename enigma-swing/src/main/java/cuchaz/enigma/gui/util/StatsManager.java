package cuchaz.enigma.gui.util;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.stats.StatsGenerator;
import cuchaz.enigma.stats.StatsResult;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * A class to manage generated stats for class tree classes. This is used to avoid generating stats for a class multiple times.
 */
public class StatsManager {
	private final HashMap<ClassEntry, StatsResult> results = new HashMap<>();
	private final Map<ClassEntry, CountDownLatch> latches = new HashMap<>();
	private StatsGenerator generator;

	public StatsManager() {
		this.generator = null;
	}

	/**
	 * Gets the current {@link StatsGenerator}.
	 *
	 * @return the current generator
	 */
	public StatsGenerator getGenerator() {
		return this.generator;
	}

	/**
	 * Sets the stats generator to use for generating stats. This should be called whenever the underlying {@link cuchaz.enigma.EnigmaProject} is updated.
	 *
	 * @param generator the generator to use
	 */
	public void setStatsGenerator(StatsGenerator generator) {
		this.generator = generator;
	}

	/**
	 * Generates stats for the given class entry. If stats are already being generated for that entry, this method will block until the stats are generated to preserve processing power.
	 *
	 * <p>When complete, the stats will be stored in this manager and are ready for use.
	 *
	 * @param classEntry the entry to generate stats for
	 */
	public void generateFor(ClassEntry classEntry) {
		if (!this.latches.containsKey(classEntry)) {
			this.latches.put(classEntry, new CountDownLatch(1));

			StatsResult stats = this.generator.generateForClassTree(ProgressListener.none(), classEntry, false);
			this.setStats(classEntry, stats);
		} else {
			try {
				this.latches.get(classEntry).await();
			} catch (InterruptedException e) {
				Logger.error(e, "Failed to await stats generation for class \"{}\"!", classEntry);
			}
		}
	}

	/**
	 * Sets the stats for the given class entry.
	 *
	 * @param classEntry the entry
	 * @param stats the stats to associate
	 */
	public void setStats(ClassEntry classEntry, StatsResult stats) {
		this.results.put(classEntry, stats);
		if (this.latches.containsKey(classEntry)) {
			this.latches.get(classEntry).countDown();
			this.latches.remove(classEntry);
		}
	}

	/**
	 * Gets the stats for the given class entry.
	 *
	 * @param classEntry the entry to get stats for
	 * @return the stats for the given class entry, or {@code null} if not yet generated
	 */
	public StatsResult getStats(ClassEntry classEntry) {
		return this.results.get(classEntry);
	}
}

package cuchaz.enigma.gui.util;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
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
	 * Generates stats for the given class node. If stats are already being generated for that node's class entry, this method will block until the stats are generated to preserve processing power.
	 *
	 * <p>When complete, the stats will be stored in this manager and are ready for use.
	 *
	 * @param node the node to generate stats for
	 */
	public void generateFor(ClassSelectorClassNode node) {
		ClassEntry entry = node.getObfEntry();

		if (!this.latches.containsKey(entry)) {
			this.latches.put(entry, new CountDownLatch(1));

			StatsResult stats = this.generator.generateForClassTree(ProgressListener.none(), entry, false);
			this.setStats(node, stats);
		} else {
			try {
				this.latches.get(entry).await();
			} catch (InterruptedException e) {
				Logger.error(e, "Failed to await stats generation for class \"{}\"!", entry);
			}
		}
	}

	/**
	 * Sets the stats for the given class node.
	 *
	 * @param node the node to set stats for
	 * @param stats the stats to associate
	 */
	public void setStats(ClassSelectorClassNode node, StatsResult stats) {
		ClassEntry entry = node.getObfEntry();

		this.results.put(entry, stats);
		if (this.latches.containsKey(entry)) {
			this.latches.get(entry).countDown();
			this.latches.remove(entry);
		}
	}

	/**
	 * Invalidates all stats stored in this manager by clearing them from storage.
	 */
	public void invalidateStats() {
		this.results.clear();
	}

	/**
	 * Gets the stats for the given class node.
	 *
	 * @param node the node to get stats for
	 * @return the stats for the given class node, or {@code null} if not yet generated
	 */
	public StatsResult getStats(ClassSelectorClassNode node) {
		ClassEntry entry = node.getObfEntry();
		return this.results.get(entry);
	}
}

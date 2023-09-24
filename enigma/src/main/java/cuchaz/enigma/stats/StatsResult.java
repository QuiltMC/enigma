package cuchaz.enigma.stats;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public record StatsResult(@Nullable ClassEntry obfEntry, Map<StatType, Integer> totalMappable, Map<StatType, Integer> totalUnmapped) implements StatsProvider {
	@Override
	public int getMappable(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return this.getSum(this.totalMappable, types);
	}

	@Override
	public int getUnmapped(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return this.getSum(this.totalUnmapped, types);
	}

	private int getSum(Map<StatType, Integer> map, StatType... types) {
		int sum = 0;
		for (StatType type : types) {
			if (this.getTypes().contains(type)) {
				sum += map.getOrDefault(type, 0);
			}
		}

		return sum;
	}

	@Override
	public int getMapped(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return this.getMappable(types) - this.getUnmapped(types);
	}

	/**
	 * Gets the set of {@link StatType}s that were considered when producing this result.
	 *
	 * @return the set of types
	 */
	public Set<StatType> getTypes() {
		return this.totalMappable.keySet();
	}

	/**
	 * Gets a tree representation of unmapped entries, formatted to JSON. This is used to show a graph of entries that need mapping.
	 *
	 * @return the tree of unmapped entries as JSON
	 */
	public String getTreeJson() {
		return null;
		//return new GsonBuilder().setPrettyPrinting().create().toJson(this.tree.root);
	}

	@Override
	public String toString() {
		return this.toString(StatType.values());
	}

	/**
	 * Produces a clean string representation of this result, taking into consideration only the provided {@link StatType}s.
	 * The result is formatted as {@code <mapped>/<mappable> <percentage>%}.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the string representation
	 */
	public String toString(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return String.format("%s/%s %.1f%%", this.getMapped(types), this.getMappable(types), this.getPercentage(types));
	}
}

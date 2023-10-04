package org.quiltmc.enigma.stats;

public interface StatsProvider {
	/**
	 * Gets the total number of entries that can be mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of mappable entries for the given types
	 */
	int getMappable(StatType... types);

	/**
	 * Gets the total number of entries that are mappable and remain obfuscated, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of unmapped entries for the given types
	 */
	int getUnmapped(StatType... types);

	/**
	 * Gets the total number of entries that have been mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of mapped entries for the given types
	 */
	int getMapped(StatType... types);

	/**
	 * Gets the percentage of entries that have been mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the percentage of entries mapped for the given types
	 */
	default double getPercentage(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		// avoid showing "Nan%" when there are no entries to map
		// if there are none, you've mapped them all!
		int mappable = this.getMappable(types);
		if (mappable == 0) {
			return 100.0f;
		}

		return (this.getMapped(types) * 100.0f) / mappable;
	}
}

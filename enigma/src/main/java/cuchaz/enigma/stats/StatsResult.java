package cuchaz.enigma.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record StatsResult(Map<StatType, Integer> totalMappable, Map<StatType, Integer> totalUnmapped, Map<StatType, Map<String, Integer>> unmappedTreeData, boolean isPackage) implements StatsProvider {
	public static StatsResult create(Map<StatType, Integer> totalMappable, Map<StatType, Map<String, Integer>> unmappedTreeData, boolean isPackage) {
		Map<StatType, Integer> totalUnmapped = new HashMap<>();
		for (var entry : unmappedTreeData.entrySet()) {
			for (int value : entry.getValue().values()) {
				totalUnmapped.put(entry.getKey(), totalUnmapped.getOrDefault(entry.getKey(), 0) + value);
			}
		}

		return new StatsResult(totalMappable, totalUnmapped, unmappedTreeData, isPackage);
	}

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

	public StatsTree<Integer> buildTree(String topLevelPackageDot, Set<StatType> includedTypes) {
		StatsTree<Integer> tree = new StatsTree<>();

		for (Map.Entry<StatType, Map<String, Integer>> typedEntry : this.unmappedTreeData.entrySet()) {
			if (!includedTypes.contains(typedEntry.getKey())) {
				continue;
			}

			for (Map.Entry<String, Integer> entry : typedEntry.getValue().entrySet()) {
				if (entry.getKey().startsWith(topLevelPackageDot)) {
					StatsTree.Node<Integer> node = tree.getNode(entry.getKey());
					int value = node.getValue() == null ? 0 : node.getValue();

					node.setValue(value + entry.getValue());
				}
			}
		}

		tree.collapse(tree.root);
		return tree;
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

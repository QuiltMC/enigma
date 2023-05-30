package cuchaz.enigma.stats;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StatsResult {
	private final Map<StatType, Integer> totalMappable;
	private final Map<StatType, Integer> totalUnmapped;
	private final Tree<Integer> tree;

	public StatsResult(Map<StatType, Integer> totalMappable, Map<StatType, Integer> totalUnmapped, Tree<Integer> tree) {
		this.totalMappable = totalMappable;
		this.totalUnmapped = totalUnmapped;
		this.tree = tree;
	}

	/**
	 * Gets the total number of entries that can be mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of mappable entries for the given types
	 */
	public int getMappable(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return this.getSum(this.totalMappable, types);
	}

	/**
	 * Gets the total number of entries that are mappable and remain obfuscated, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of unmapped entries for the given types
	 */
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

	/**
	 * Gets the total number of entries that have been mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the number of mapped entries for the given types
	 */
	public int getMapped(StatType... types) {
		if (types.length == 0) {
			types = StatType.values();
		}

		return this.getMappable(types) - this.getUnmapped(types);
	}

	/**
	 * Gets the percentage of entries that have been mapped, taking into consideration only the provided {@link StatType}s.
	 * Defaults to all types if none are provided.
	 *
	 * @param types the types of entry to include in the result
	 * @return the percentage of entries mapped for the given types
	 */
	public double getPercentage(StatType... types) {
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
		return new GsonBuilder().setPrettyPrinting().create().toJson(this.tree.root);
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

	public static class Tree<T> {
		public final Node<T> root;
		private final Map<String, Node<T>> nodes = new HashMap<>();

		public static class Node<T> {
			private String name;
			private T value;
			private List<Node<T>> children = new ArrayList<>();
			private final Map<String, Node<T>> namedChildren = new HashMap<>();

			public Node(String name, T value) {
				this.name = name;
				this.value = value;
			}

			public T getValue() {
				return this.value;
			}

			public void setValue(T value) {
				this.value = value;
			}
		}

		public Tree() {
			this.root = new Node<>("", null);
		}

		public Node<T> getNode(String name) {
			Node<T> node = this.nodes.get(name);

			if (node == null) {
				node = this.root;

				for (String part : name.split("\\.")) {
					Node<T> child = node.namedChildren.get(part);

					if (child == null) {
						child = new Node<>(part, null);
						node.namedChildren.put(part, child);
						node.children.add(child);
					}

					node = child;
				}

				this.nodes.put(name, node);
			}

			return node;
		}

		public void collapse(Node<T> node) {
			while (node.children.size() == 1) {
				Node<T> child = node.children.get(0);
				node.name = node.name.isEmpty() ? child.name : node.name + "." + child.name;
				node.children = child.children;
				node.value = child.value;
			}

			for (Node<T> child : node.children) {
				this.collapse(child);
			}
		}
	}
}

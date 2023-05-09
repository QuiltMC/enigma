package cuchaz.enigma.gui.stats;

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

	public int getMappable(StatType... types) {
		return this.getSum(this.totalMappable, types);
	}

	public int getUnmapped(StatType... types) {
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

	public int getMapped(StatType... types) {
		return this.getMappable(types) - this.getUnmapped(types);
	}

	public double getPercentage(StatType... types) {
		// avoid showing "Nan%" when there are no entries to map
		// if there are none, you've mapped them all!
		int mappable = this.getMappable(types);
		if (mappable == 0) {
			return 100.0f;
		}

		return (this.getMapped(types) * 100.0f) / mappable;
	}

	public Set<StatType> getTypes() {
		return this.totalMappable.keySet();
	}

	public String getTreeJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this.tree.root);
	}

	@Override
	public String toString() {
		return this.toString(StatType.values());
	}

	public String toString(StatType... types) {
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

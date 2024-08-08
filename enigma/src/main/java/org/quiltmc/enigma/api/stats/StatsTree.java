package org.quiltmc.enigma.api.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsTree<T> {
	public final Node<T> root;
	private final Map<String, Node<T>> nodes = new HashMap<>();

	public static class Node<T> {
		private String name;
		private T value;
		private List<Node<T>> children = new ArrayList<>();
		private final transient Map<String, Node<T>> namedChildren = new HashMap<>();

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

	public StatsTree() {
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

package org.quiltmc.enigma.util;

import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;

import java.io.PrintWriter;

public final class EntryTreePrinter {
	private final PrintWriter output;

	public EntryTreePrinter(PrintWriter output) {
		this.output = output;
	}

	public static void print(PrintWriter output, EntryTree<?> tree) {
		var printer = new EntryTreePrinter(output);
		printer.print(tree);
	}

	public <T> void print(EntryTree<T> tree) {
		this.output.println(tree);
		this.output.flush();

		var iterator = tree.getRootNodes().iterator();
		while (iterator.hasNext()) {
			var node = iterator.next();
			this.printNode(node, iterator.hasNext(), "");
		}
	}

	private <T> void printNode(EntryTreeNode<T> node, boolean hasNext, String indent) {
		this.output.print(indent);

		this.output.print(hasNext ? "├── " : "└── ");
		this.output.print(node.getEntry());

		if (node.hasValue()) {
			this.output.print(" -> ");
			this.output.println(node.getValue());
		} else {
			this.output.println();
		}

		this.output.flush();

		var iterator = node.getChildNodes().iterator();
		while (iterator.hasNext()) {
			var child = iterator.next();
			var childIndent = indent + (hasNext ? "│\u00A0\u00A0 " : "    "); // \u00A0 is non-breaking space

			this.printNode(child, iterator.hasNext(), childIndent);
		}
	}
}

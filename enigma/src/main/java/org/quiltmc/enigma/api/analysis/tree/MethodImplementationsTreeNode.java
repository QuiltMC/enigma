package org.quiltmc.enigma.api.analysis.tree;

import org.quiltmc.enigma.api.analysis.index.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.JarIndex;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodImplementationsTreeNode extends AbstractMethodTreeNode {
	public MethodImplementationsTreeNode(Translator translator, MethodEntry entry) {
		super(translator, entry);
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
	}

	public static MethodImplementationsTreeNode findNode(MethodImplementationsTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodImplementationsTreeNode foundNode = findNode((MethodImplementationsTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = this.translator.translate(this.entry);
		return translatedEntry.getFullName() + "()";
	}

	public void load(JarIndex index) {
		// get all method implementations
		List<MethodImplementationsTreeNode> nodes = new ArrayList<>();
		EntryIndex entryIndex = index.getEntryIndex();
		InheritanceIndex inheritanceIndex = index.getInheritanceIndex();

		Collection<ClassEntry> descendants = inheritanceIndex.getDescendants(this.entry.getParent());
		for (ClassEntry inheritor : descendants) {
			MethodEntry methodEntry = this.entry.withParent(inheritor);
			if (entryIndex.hasMethod(methodEntry)) {
				nodes.add(new MethodImplementationsTreeNode(this.translator, methodEntry));
			}
		}

		// add them to this node
		nodes.forEach(this::add);
	}
}

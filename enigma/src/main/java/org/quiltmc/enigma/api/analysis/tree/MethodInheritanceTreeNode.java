package org.quiltmc.enigma.api.analysis.tree;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

public class MethodInheritanceTreeNode extends AbstractMethodTreeNode {
	private final boolean implemented;

	public MethodInheritanceTreeNode(Translator translator, MethodEntry entry, boolean implemented) {
		super(translator, entry);
		this.implemented = implemented;
	}

	public static MethodInheritanceTreeNode findNode(MethodInheritanceTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.getMethodEntry().equals(entry)) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			MethodInheritanceTreeNode foundNode = findNode((MethodInheritanceTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}

		return null;
	}

	public boolean isImplemented() {
		return this.implemented;
	}

	@Override
	public String toString() {
		MethodEntry translatedEntry = this.translator.translate(this.entry);

		if (!this.implemented) {
			return translatedEntry.getParent().getFullName();
		} else {
			return translatedEntry.getFullName() + "()";
		}
	}

	/**
	 * Returns true if there is sub-node worthy to display.
	 */
	public boolean load(JarIndex index) {
		// get all the child nodes
		EntryIndex entryIndex = index.getIndex(EntryIndex.class);
		InheritanceIndex inheritanceIndex = index.getIndex(InheritanceIndex.class);

		boolean ret = false;
		for (ClassEntry inheritorEntry : inheritanceIndex.getChildren(this.entry.getParent())) {
			MethodEntry methodEntry = new MethodEntry(inheritorEntry, this.entry.getName(), this.entry.getDesc());

			MethodInheritanceTreeNode node = new MethodInheritanceTreeNode(this.translator, methodEntry, entryIndex.hasMethod(methodEntry));
			boolean childOverride = node.load(index);

			if (childOverride || node.implemented) {
				this.add(node);
				ret = true;
			}
		}

		return ret;
	}
}

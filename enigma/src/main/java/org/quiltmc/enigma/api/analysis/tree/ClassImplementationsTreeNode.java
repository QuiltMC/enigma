package org.quiltmc.enigma.api.analysis.tree;

import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassImplementationsTreeNode extends AbstractClassTreeNode {
	public ClassImplementationsTreeNode(Translator translator, ClassEntry entry) {
		super(translator, entry);
	}

	public static ClassImplementationsTreeNode findNode(ClassImplementationsTreeNode node, MethodEntry entry) {
		// is this the node?
		if (node.entry.equals(entry.getParent())) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassImplementationsTreeNode foundNode = findNode((ClassImplementationsTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}

		return null;
	}

	public void load(JarIndex index) {
		// get all method implementations
		List<ClassImplementationsTreeNode> nodes = new ArrayList<>();
		InheritanceIndex inheritanceIndex = index.getIndex(InheritanceIndex.class);

		Collection<ClassEntry> inheritors = inheritanceIndex.getChildren(this.entry);
		for (ClassEntry inheritor : inheritors) {
			nodes.add(new ClassImplementationsTreeNode(this.translator, inheritor));
		}

		// add them to this node
		nodes.forEach(this::add);
	}
}

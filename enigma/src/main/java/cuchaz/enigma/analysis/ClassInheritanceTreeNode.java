package cuchaz.enigma.analysis;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.ArrayList;
import java.util.List;

public class ClassInheritanceTreeNode extends AbstractClassTreeNode {
	public ClassInheritanceTreeNode(Translator translator, String obfClassName) {
		super(translator, new ClassEntry(obfClassName));
	}

	public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
		// is this the node?
		if (node.getClassName().equals(entry.getFullName())) {
			return node;
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			ClassInheritanceTreeNode foundNode = findNode((ClassInheritanceTreeNode) node.getChildAt(i), entry);
			if (foundNode != null) {
				return foundNode;
			}
		}

		return null;
	}

	public void load(InheritanceIndex ancestries, boolean recurse) {
		// get all the child nodes
		List<ClassInheritanceTreeNode> nodes = new ArrayList<>();
		for (ClassEntry inheritor : ancestries.getChildren(this.entry)) {
			nodes.add(new ClassInheritanceTreeNode(this.translator, inheritor.getFullName()));
		}

		// add them to this node
		nodes.forEach(this::add);

		if (recurse) {
			for (ClassInheritanceTreeNode node : nodes) {
				node.load(ancestries, true);
			}
		}
	}
}

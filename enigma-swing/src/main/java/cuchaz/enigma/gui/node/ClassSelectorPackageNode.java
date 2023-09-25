package cuchaz.enigma.gui.node;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class ClassSelectorPackageNode extends SortedMutableTreeNode {
	private final String packageName;

	public ClassSelectorPackageNode(Comparator<TreeNode> comparator, String packageName) {
		super(comparator);

		this.packageName = packageName != null ? packageName : "(none)";
	}

	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String toString() {
		return !this.packageName.equals("(none)") ? ClassEntry.getNameInPackage(this.packageName) : "(none)";
	}
}

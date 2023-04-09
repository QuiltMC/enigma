package cuchaz.enigma.gui.node;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class ClassSelectorPackageNode extends SortedMutableTreeNode {
	private String packageName;

	public ClassSelectorPackageNode(Comparator<TreeNode> comparator, String packageName) {
		super(comparator);

		this.packageName = packageName != null ? packageName : "(none)";
	}

	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public Object getUserObject() {
		return this.packageName;
	}

	@Override
	public void setUserObject(Object userObject) {
		if (userObject instanceof String) {
			this.packageName = (String) userObject;
		}

		super.setUserObject(userObject);
	}

	@Override
	public String toString() {
		return !this.packageName.equals("(none)") ? ClassEntry.getNameInPackage(this.packageName) : "(none)";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassSelectorPackageNode && this.equals((ClassSelectorPackageNode) other);
	}

	@Override
	public int hashCode() {
		return this.packageName.hashCode();
	}

	public boolean equals(ClassSelectorPackageNode other) {
		return other != null && this.packageName.equals(other.packageName);
	}
}

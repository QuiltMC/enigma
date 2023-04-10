package cuchaz.enigma.gui;

import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.gui.node.SortedMutableTreeNode;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class NestedPackages {
	private final SortedMutableTreeNode root;
	private final Map<String, SortedMutableTreeNode> packageToNode = new HashMap<>();
	private final Map<ClassEntry, ClassSelectorClassNode> classToNode = new HashMap<>();
	private final EntryRemapper remapper;
	private final Comparator<TreeNode> comparator;

	public NestedPackages(Iterable<ClassEntry> entries, Comparator<ClassEntry> entryComparator, EntryRemapper remapper) {
		this.remapper = remapper;
		this.comparator = (a, b) -> {
			if (a instanceof ClassSelectorPackageNode pA) {
				if (b instanceof ClassSelectorPackageNode pB) {
					return pA.getPackageName().compareTo(pB.getPackageName());
				} else {
					return -1;
				}
			} else if (a instanceof ClassSelectorClassNode cA) {
				if (b instanceof ClassSelectorClassNode cB) {
					return entryComparator.compare(cA.getClassEntry(), cB.getClassEntry());
				} else {
					return 1;
				}
			}

			return 0;
		};
		this.root = new SortedMutableTreeNode(this.comparator);

		for (var entry : entries) {
			this.addEntry(entry);
		}
	}

	public void addEntry(ClassEntry entry) {
		var translated = this.remapper.deobfuscate(entry);
		var me = new ClassSelectorClassNode(entry, translated);
		this.classToNode.put(entry, me);
		SortedMutableTreeNode parent = this.getPackage(translated.getPackageName());
		parent.insert(me, 0);
	}

	public SortedMutableTreeNode getPackage(String packageName) {
		var node = this.packageToNode.get(packageName);

		if (packageName == null) {
			return this.root;
		}

		if (node == null) {
			node = new ClassSelectorPackageNode(this.comparator, packageName);
			SortedMutableTreeNode parent = this.getPackage(ClassEntry.getParentPackage(packageName));
			parent.insert(node, 0);
			this.packageToNode.put(packageName, node);
		}

		return node;
	}

	public SortedMutableTreeNode getRoot() {
		return this.root;
	}

	public TreePath getPackagePath(String packageName) {
		var node = this.packageToNode.getOrDefault(packageName, this.root);
		return new TreePath(node.getPath());
	}

	public ClassSelectorClassNode getClassNode(ClassEntry entry) {
		return this.classToNode.get(entry);
	}

	public void removeClassNode(ClassEntry entry) {
		var node = this.classToNode.remove(entry);

		if (node != null) {
			var parent = node.getParent();
			node.removeFromParent();
			// remove dangling packages
			var packageNode = (SortedMutableTreeNode) parent;

			while (packageNode != null && packageNode.getChildCount() == 0) {
				var theNode = packageNode;
				packageNode = (SortedMutableTreeNode) packageNode.getParent();
				theNode.removeFromParent();

				if (theNode instanceof ClassSelectorPackageNode pn) {
					this.packageToNode.remove(pn.getPackageName());
				}
			}
		}
	}

	public Collection<ClassEntry> getClassEntries() {
		return this.classToNode.keySet();
	}

	public Collection<SortedMutableTreeNode> getPackageNodes() {
		return this.packageToNode.values();
	}
}

package cuchaz.enigma.gui.util;

import com.google.common.collect.Iterables;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * A MutableTreeNode whose contents are always guaranteed to be sorted with the given comparator.
 */
public class SortedMutableTreeNode implements MutableTreeNode {
	private final Comparator<TreeNode> comparator;
	private MutableTreeNode parent;
	private final List<TreeNode> children;
	private boolean isSorted = true;

	public SortedMutableTreeNode(Comparator<TreeNode> comparator) {
		this.comparator = comparator;
		this.children = new ArrayList<>();
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		if (child == null) throw new IllegalArgumentException("child is null");

		MutableTreeNode oldParent = (MutableTreeNode) child.getParent();

		if (oldParent != null) oldParent.remove(child);
		child.setParent(this);
		children.add(child);
		isSorted = false;
	}

	private void checkSorted() {
		if (!isSorted) {
			isSorted = true;
			children.sort(comparator);
		}
	}

	@Override
	public void remove(int index) {
		checkSorted();

		remove((MutableTreeNode) getChildAt(index));
	}

	@Override
	public void remove(MutableTreeNode node) {
		children.remove(node);
		node.setParent(null);
	}

	@Override
	public void setUserObject(Object object) {
		throw new IllegalStateException("SortedMutableTreeNodes can't have user objects.");
	}

	@Override
	public void removeFromParent() {
		if (parent != null)
			parent.remove(this);
	}

	@Override
	public void setParent(MutableTreeNode newParent) {
		parent = newParent;
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		checkSorted();

		return children.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		return Iterables.indexOf(children, other -> comparator.compare(node, other) == 0);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return children.isEmpty();
	}

	@Override
	public Enumeration<? extends TreeNode> children() {
		var iter = children.iterator();

		return new Enumeration<>() {
			@Override
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			@Override
			public TreeNode nextElement() {
				return iter.next();
			}
		};
	}

	public TreeNode[] getPath() {
		return doGetPath(this, 0);
	}

	private static TreeNode[] doGetPath(TreeNode at, int depth) {
		if (at == null) {
			return new TreeNode[depth];
		}

		TreeNode[] path = doGetPath(at, depth + 1);
		path[path.length - depth - 1] = at;
		return path;
	}
}

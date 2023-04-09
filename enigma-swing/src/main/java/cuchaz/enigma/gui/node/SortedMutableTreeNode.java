package cuchaz.enigma.gui.node;

import com.google.common.collect.Iterables;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * A MutableTreeNode whose contents are always guaranteed to be sorted with the given comparator.
 */
public class SortedMutableTreeNode extends DefaultMutableTreeNode {
	private final Comparator<TreeNode> comparator;
	private final List<TreeNode> children;
	private boolean isSorted = true;

	public SortedMutableTreeNode(Comparator<TreeNode> comparator) {
		this.comparator = comparator;
		this.children = new ArrayList<>();
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		if (child == null) {
			throw new IllegalArgumentException("child is null");
		}

		MutableTreeNode oldParent = (MutableTreeNode) child.getParent();

		if (oldParent != null) {
			oldParent.remove(child);
		}

		child.setParent(this);
		this.children.add(child);
		this.isSorted = false;
	}

	private void checkSorted() {
		if (!this.isSorted) {
			this.isSorted = true;
			this.children.sort(this.comparator);
		}
	}

	@Override
	public void remove(int index) {
		this.checkSorted();

		this.remove((MutableTreeNode) this.getChildAt(index));
	}

	@Override
	public void remove(MutableTreeNode node) {
		this.children.remove(node);
		node.setParent(null);
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		this.checkSorted();

		return this.children.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return this.children.size();
	}

	@Override
	public int getIndex(TreeNode node) {
		return Iterables.indexOf(this.children, other -> this.comparator.compare(node, other) == 0);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return this.children.isEmpty();
	}

	@Override
	public Enumeration<TreeNode> children() {
		var iter = this.children.iterator();

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
}

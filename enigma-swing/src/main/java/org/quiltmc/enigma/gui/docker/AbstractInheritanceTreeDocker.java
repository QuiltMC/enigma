package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.api.analysis.tree.AbstractClassTreeNode;
import org.quiltmc.enigma.api.analysis.tree.AbstractMethodTreeNode;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.SingleTreeSelectionModel;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.I18n;

import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

public abstract class AbstractInheritanceTreeDocker extends Docker {
	private final JTree tree = new JTree();
	private final JLabel inactiveLabel = new JLabel();
	private final JLabel notFoundLabel = new JLabel();
	private final JPanel textPanel;
	private final JPanel treePanel;
	private final String inactiveTextKey;
	private final String notFoundKey;

	protected AbstractInheritanceTreeDocker(Gui gui, TreeCellRenderer cellRenderer, String inactiveTextKey, String notFoundKey) {
		super(gui);

		this.textPanel = new JPanel(new BorderLayout());
		this.textPanel.add(this.inactiveLabel, BorderLayout.NORTH);

		this.treePanel = new JPanel(new BorderLayout());
		this.treePanel.add(new JScrollPane(tree));

		this.inactiveTextKey = inactiveTextKey;
		this.notFoundKey = notFoundKey;
		this.retranslateUi();
		this.add(this.textPanel);

		this.tree.setModel(null);
		this.tree.setCellRenderer(cellRenderer);
		this.tree.setSelectionModel(new SingleTreeSelectionModel());
		this.tree.setShowsRootHandles(true);
		this.tree.addMouseListener(GuiUtil.onMouseClick(this::onClick));
	}

	private void onClick(MouseEvent event) {
		if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
			// get the selected node
			TreePath path = this.tree.getSelectionPath();
			if (path == null) {
				return;
			}

			Object node = path.getLastPathComponent();
			if (node instanceof AbstractClassTreeNode classNode) {
				this.gui.getController().navigateTo(classNode.getClassEntry());
			} else if (node instanceof AbstractMethodTreeNode methodNode) {
				this.gui.getController().navigateTo(methodNode.getMethodEntry());
			}
		}
	}

	private void setupTree(@Nullable TreeModel model) {
		this.tree.setModel(model);
		if (model != null) {
			this.remove(this.textPanel);
			this.add(this.treePanel);
		} else {
			this.textPanel.remove(this.inactiveLabel);
			this.textPanel.add(this.notFoundLabel, BorderLayout.NORTH);
		}
	}

	public void display(Entry<?> entry) {
		this.tree.setModel(null);

		DefaultMutableTreeNode node = this.getNodeFor(entry);

		if (node != null) {
			// show the tree at the root
			TreePath path = GuiUtil.getPathToRoot(node);
			TreeNode root = (TreeNode) path.getPathComponent(0);
			if (root.children().hasMoreElements()) { // do not display a tree with no inheritance (only a root entry)
				this.setupTree(new DefaultTreeModel(root));
				this.tree.expandPath(path);
				this.tree.setSelectionRow(this.tree.getRowForPath(path));
			} else {
				this.setupTree(null);
			}
		} else {
			this.setupTree(null);
		}

		this.setVisible(true);
	}

	@Nullable
	protected abstract DefaultMutableTreeNode getNodeFor(Entry<?> entry);

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.RIGHT, VerticalLocation.TOP);
	}

	@Override
	public void retranslateUi() {
		this.inactiveLabel.setText(I18n.translate(this.inactiveTextKey));
		this.notFoundLabel.setText(I18n.translate(this.notFoundKey));
	}
}

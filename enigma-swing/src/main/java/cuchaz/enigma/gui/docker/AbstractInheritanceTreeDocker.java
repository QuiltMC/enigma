package cuchaz.enigma.gui.docker;

import cuchaz.enigma.analysis.AbstractClassTreeNode;
import cuchaz.enigma.analysis.AbstractMethodTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;

public abstract class AbstractInheritanceTreeDocker extends Docker {
	private final JTree tree = new JTree();

	protected AbstractInheritanceTreeDocker(Gui gui, TreeCellRenderer cellRenderer) {
		super(gui);

		this.tree.setModel(null);
		this.tree.setCellRenderer(cellRenderer);
		this.tree.setSelectionModel(new SingleTreeSelectionModel());
		this.tree.setShowsRootHandles(true);
		this.tree.addMouseListener(GuiUtil.onMouseClick(this::onClick));

		this.add(new JScrollPane(this.tree));
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

	public void display(Entry<?> entry) {
		this.tree.setModel(null);

		DefaultMutableTreeNode node = this.getNodeFor(entry);

		if (node != null) {
			// show the tree at the root
			TreePath path = GuiUtil.getPathToRoot(node);
			this.tree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			this.tree.expandPath(path);
			this.tree.setSelectionRow(this.tree.getRowForPath(path));
		}

		this.setVisible(true);
	}

	@Nullable
	protected abstract DefaultMutableTreeNode getNodeFor(Entry<?> entry);

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.RIGHT, VerticalLocation.TOP);
	}
}

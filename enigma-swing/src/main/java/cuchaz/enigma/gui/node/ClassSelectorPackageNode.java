package cuchaz.enigma.gui.node;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.stats.StatsGenerator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class ClassSelectorPackageNode extends SortedMutableTreeNode {
	private final String packageName;

	public ClassSelectorPackageNode(Comparator<TreeNode> comparator, String packageName) {
		super(comparator);

		this.packageName = packageName != null ? packageName : "(none)";
	}

	public void reloadStats(Gui gui, ClassSelector selector) {
		StatsGenerator generator = gui.getController().getStatsGenerator();
		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) selector.getCellRenderer();
		this.updateIcon(generator, renderer);

		// todo needs to be recursive and restore
		SwingUtilities.invokeLater(() -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.getParent();
			while (!node.isRoot() && node instanceof ClassSelectorPackageNode packageNode) {
				packageNode.updateIcon(generator, renderer);
				node = (DefaultMutableTreeNode) packageNode.getParent();
			}

			var expansionState = selector.getExpansionState();
			selector.reload();
			selector.restoreExpansionState(expansionState);
		});
	}

	private void updateIcon(StatsGenerator generator, DefaultTreeCellRenderer renderer) {
		System.out.println("updating icon" + this.getPackageName());
		renderer.setIcon(GuiUtil.getDeobfuscationIcon(generator.getResultNullable(), this.getPackageName()));
	}

	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String toString() {
		return !this.packageName.equals("(none)") ? ClassEntry.getNameInPackage(this.packageName) : "(none)";
	}
}

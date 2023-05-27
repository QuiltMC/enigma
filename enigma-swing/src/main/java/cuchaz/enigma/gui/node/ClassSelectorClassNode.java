package cuchaz.enigma.gui.node;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.stats.StatsManager;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class ClassSelectorClassNode extends SortedMutableTreeNode {
	private final ClassEntry obfEntry;
	private ClassEntry classEntry;

	public ClassSelectorClassNode(ClassEntry obfEntry, ClassEntry classEntry) {
		super(Comparator.comparing(TreeNode::toString));
		this.obfEntry = obfEntry;
		this.classEntry = classEntry;
		this.setUserObject(classEntry);
	}

	public ClassEntry getObfEntry() {
		return this.obfEntry;
	}

	public ClassEntry getClassEntry() {
		return this.classEntry;
	}

	/**
	 * Reloads the stats for this class node and updates the icon in the provided class selector.
	 * @param gui the current gui instance
	 * @param selector the class selector to reload on
	 * @param updateIfPresent whether to update the stats if they have already been generated for this node
	 */
	public void reloadStats(Gui gui, ClassSelector selector, boolean updateIfPresent) {
		StatsManager manager = gui.getStatsManager();
		ClassEntry entry = this.getClassEntry();

		SwingWorker<ClassSelectorClassNode, Void> iconUpdateWorker = new SwingWorker<>() {
			@Override
			protected ClassSelectorClassNode doInBackground() {
				if (manager.getStats(entry) == null || updateIfPresent) {
					manager.generateFor(entry);
				}

				return ClassSelectorClassNode.this;
			}

			@Override
			public void done() {
				((DefaultTreeCellRenderer) selector.getCellRenderer()).setIcon(GuiUtil.getDeobfuscationIcon(manager.getStats(entry)));
				SwingUtilities.invokeLater(() -> selector.reload(ClassSelectorClassNode.this, false));
			}
		};

		SwingUtilities.invokeLater(iconUpdateWorker::execute);
	}

	@Override
	public String toString() {
		return this.classEntry.getSimpleName();
	}

	@Override
	public Object getUserObject() {
		return this.classEntry;
	}

	@Override
	public void setUserObject(Object userObject) {
		String packageName = "";
		if (this.classEntry.getPackageName() != null) {
			packageName = this.classEntry.getPackageName() + "/";
		}

		if (userObject instanceof String) {
			this.classEntry = new ClassEntry(packageName + userObject);
		} else if (userObject instanceof ClassEntry entry) {
			this.classEntry = entry;
		}

		super.setUserObject(this.classEntry);
	}
}

package cuchaz.enigma.gui.node;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.StatsManager;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class ClassSelectorClassNode extends SortedMutableTreeNode {
	private final ClassEntry obfEntry;
	private ClassEntry deobfEntry;

	public ClassSelectorClassNode(ClassEntry obfEntry, ClassEntry deobfEntry) {
		super(Comparator.comparing(TreeNode::toString));
		this.obfEntry = obfEntry;
		this.deobfEntry = deobfEntry;
		this.setUserObject(deobfEntry);
	}

	public ClassEntry getObfEntry() {
		return this.obfEntry;
	}

	public ClassEntry getDeobfEntry() {
		return this.deobfEntry;
	}

	/**
	 * Reloads the stats for this class node and updates the icon in the provided class selector.
	 *
	 * @param gui the current gui instance
	 * @param selector the class selector to reload on
	 * @param updateIfPresent whether to update the stats if they have already been generated for this node
	 */
	public void reloadStats(Gui gui, ClassSelector selector, boolean updateIfPresent) {
		StatsManager manager = gui.getStatsManager();

		SwingWorker<ClassSelectorClassNode, Void> iconUpdateWorker = new SwingWorker<>() {
			@Override
			protected ClassSelectorClassNode doInBackground() {
				if (manager.getStats(ClassSelectorClassNode.this) == null || updateIfPresent) {
					manager.generateFor(ClassSelectorClassNode.this);
				}

				return ClassSelectorClassNode.this;
			}

			@Override
			public void done() {
				((DefaultTreeCellRenderer) selector.getCellRenderer()).setIcon(GuiUtil.getDeobfuscationIcon(manager.getStats(ClassSelectorClassNode.this)));
				SwingUtilities.invokeLater(() -> selector.reload(ClassSelectorClassNode.this, false));
			}
		};

		SwingUtilities.invokeLater(iconUpdateWorker::execute);
	}

	@Override
	public String toString() {
		return this.deobfEntry.getSimpleName();
	}

	@Override
	public Object getUserObject() {
		return this.deobfEntry;
	}

	@Override
	public void setUserObject(Object userObject) {
		String packageName = "";
		if (this.deobfEntry.getPackageName() != null) {
			packageName = this.deobfEntry.getPackageName() + "/";
		}

		if (userObject instanceof String) {
			this.deobfEntry = new ClassEntry(packageName + userObject);
		} else if (userObject instanceof ClassEntry entry) {
			this.deobfEntry = entry;
		}

		super.setUserObject(this.deobfEntry);
	}
}

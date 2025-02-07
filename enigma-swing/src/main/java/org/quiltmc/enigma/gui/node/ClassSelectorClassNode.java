package org.quiltmc.enigma.gui.node;

import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

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
		StatsGenerator generator = gui.getController().getStatsGenerator();

		SwingWorker<ClassSelectorClassNode, Void> iconUpdateWorker = new SwingWorker<>() {
			@Override
			protected ClassSelectorClassNode doInBackground() {
				if (generator.getResultNullable() == null && generator.getOverallProgress() == null) {
					generator.generate(ProgressListener.createEmpty(), new StatsGenerator.GenerationParameters(EditableType.toStatTypes(gui.getEditableTypes())));
				} else if (updateIfPresent) {
					generator.generate(ProgressListener.createEmpty(), ClassSelectorClassNode.this.getObfEntry(), new StatsGenerator.GenerationParameters(EditableType.toStatTypes(gui.getEditableTypes())));
				}

				return ClassSelectorClassNode.this;
			}

			@Override
			public void done() {
				try {
					((DefaultTreeCellRenderer) selector.getCellRenderer()).setIcon(GuiUtil.getDeobfuscationIcon(generator.getResultNullable(), ClassSelectorClassNode.this.getObfEntry()));
				} catch (NullPointerException ignored) {
					// do nothing. this seems to be a race condition, likely a bug in FlatLAF caused by us suppressing the default tree icons
					// ignoring this error should never cause issues since it only occurs at startup
				}

				SwingUtilities.invokeLater(() -> selector.reload(ClassSelectorClassNode.this, false));
			}
		};

		if (Config.main().features.enableClassTreeStatIcons.value()) {
			SwingUtilities.invokeLater(iconUpdateWorker::execute);
		}
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

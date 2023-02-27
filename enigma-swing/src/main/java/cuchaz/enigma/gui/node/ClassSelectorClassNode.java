/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui.node;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsResult;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class ClassSelectorClassNode extends DefaultMutableTreeNode {
	private final ClassEntry obfEntry;
	private ClassEntry classEntry;
	private StatsResult stats;

	public ClassSelectorClassNode(ClassEntry obfEntry, ClassEntry classEntry) {
		this.obfEntry = obfEntry;
		this.classEntry = classEntry;
		this.stats = null;
		this.setUserObject(classEntry);
	}

	public ClassEntry getObfEntry() {
		return this.obfEntry;
	}

	public ClassEntry getClassEntry() {
		return this.classEntry;
	}

	public StatsResult getStats() {
		return this.stats;
	}

	public void setStats(StatsResult stats) {
		this.stats = stats;
	}

	/**
	 * Reloads the stats for this class node and updates the icon in the provided class selector.
	 * @param gui the current gui instance
	 * @param selector the class selector to reload on
	 * @param updateIfPresent whether to update the stats if they have already been generated for this node
	 */
	public void reloadStats(Gui gui, ClassSelector selector, boolean updateIfPresent) {
		SwingWorker<ClassSelectorClassNode, Void> iconUpdateWorker = new SwingWorker<>() {
			@Override
			protected ClassSelectorClassNode doInBackground() {
				if (ClassSelectorClassNode.this.getStats() == null || updateIfPresent) {
					StatsResult newStats = new StatsGenerator(gui.getController().project).generateForClassTree(ProgressListener.none(), ClassSelectorClassNode.this.getObfEntry(), false);
					ClassSelectorClassNode.this.setStats(newStats);
				}

				return ClassSelectorClassNode.this;
			}

			@Override
			public void done() {
				((DefaultTreeCellRenderer) selector.getCellRenderer()).setIcon(GuiUtil.getDeobfuscationIcon(ClassSelectorClassNode.this.getStats()));
				selector.reload(ClassSelectorClassNode.this);
			}
		};

		iconUpdateWorker.execute();
	}

	@Override
	public String toString() {
		return this.classEntry.getSimpleName();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassSelectorClassNode node && this.equals(node);
	}

	@Override
	public int hashCode() {
		return 17 + (this.classEntry != null ? this.classEntry.hashCode() : 0);
	}

	@Override
	public Object getUserObject() {
		return this.classEntry;
	}

	@Override
	public void setUserObject(Object userObject) {
		String packageName = "";
		if (this.classEntry.getPackageName() != null)
			packageName = this.classEntry.getPackageName() + "/";
		if (userObject instanceof String)
			this.classEntry = new ClassEntry(packageName + userObject);
		else if (userObject instanceof ClassEntry entry)
			this.classEntry = entry;
		super.setUserObject(this.classEntry);
	}

	public boolean equals(ClassSelectorClassNode other) {
		return this.classEntry.equals(other.classEntry);
	}
}

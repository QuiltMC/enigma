package cuchaz.enigma.gui.panels.right;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.renderer.InheritanceTreeCellRenderer;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class InheritanceTree extends AbstractInheritanceTree {
	public InheritanceTree(Gui gui) {
		super(gui, new InheritanceTreeCellRenderer(gui));
	}

	@Nullable
	@Override
	protected DefaultMutableTreeNode getNodeFor(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			return this.gui.getController().getClassInheritance(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return this.gui.getController().getMethodInheritance(methodEntry);
		}

		return null;
	}

	@Override
	public String getId() {
		return Type.INHERITANCE;
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.RIGHT, Height.FULL);
	}
}

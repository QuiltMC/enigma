package cuchaz.enigma.gui.panels.right;

import javax.annotation.Nullable;
import javax.swing.JToggleButton;
import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.renderer.ImplementationsTreeCellRenderer;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class ImplementationsTree extends AbstractInheritanceTree {
	private final JToggleButton button;

	public ImplementationsTree(Gui gui) {
		super(gui, new ImplementationsTreeCellRenderer(gui));
		this.button = new JToggleButton(this.getId());
	}

	@Nullable
	@Override
	protected DefaultMutableTreeNode getNodeFor(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			return this.gui.getController().getClassImplementations(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return this.gui.getController().getMethodImplementations(methodEntry);
		}

		return null;
	}

	@Override
	public String getId() {
		return "implementations";
	}

	@Override
	public JToggleButton getButton() {
		return this.button;
	}
}

package cuchaz.enigma.gui.panels.right;

import javax.annotation.Nullable;
import javax.swing.JToggleButton;
import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.renderer.InheritanceTreeCellRenderer;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class InheritanceTree extends AbstractInheritanceTree implements RightPanel {
	private final JToggleButton button;

	public InheritanceTree(Gui gui) {
		super(gui, new InheritanceTreeCellRenderer(gui));
		this.button = new JToggleButton(this.getId());
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
		return "inheritance";
	}

	@Override
	public JToggleButton getButton() {
		return this.button;
	}
}

package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.renderer.ImplementationsTreeCellRenderer;
import org.quiltmc.enigma.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.translation.representation.entry.Entry;
import org.quiltmc.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;

public class ImplementationsTreeDocker extends AbstractInheritanceTreeDocker {
	public ImplementationsTreeDocker(Gui gui) {
		super(gui, new ImplementationsTreeCellRenderer(gui));
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
}

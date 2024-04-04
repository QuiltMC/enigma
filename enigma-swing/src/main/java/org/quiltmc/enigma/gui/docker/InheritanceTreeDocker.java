package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.renderer.InheritanceTreeCellRenderer;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;

public class InheritanceTreeDocker extends AbstractInheritanceTreeDocker {
	public InheritanceTreeDocker(Gui gui) {
		super(gui, new InheritanceTreeCellRenderer(gui), "docker.inheritance.inactive", "docker.inheritance.not_found");
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
}

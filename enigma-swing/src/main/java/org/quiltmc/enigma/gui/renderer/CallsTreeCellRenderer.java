package org.quiltmc.enigma.gui.renderer;

import org.quiltmc.enigma.api.analysis.tree.ClassReferenceTreeNode;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.tree.FieldReferenceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodReferenceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.ReferenceTreeNode;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.UiConfig;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

public class CallsTreeCellRenderer extends DefaultTreeCellRenderer {
	private final Gui gui;

	public CallsTreeCellRenderer(Gui gui) {
		this.gui = gui;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		EntryReference<?, ?> reference = ((ReferenceTreeNode<?, ?>) value).getReference();

		this.setForeground(UiConfig.getTextColor());

		// if the node represents the method calling the entry
		if (reference != null) {
			if (reference.context instanceof MethodEntry) {
				this.setIcon(GuiUtil.getMethodIcon((MethodEntry) reference.context));
			}
		// if the node represents the called entry
		} else {
			if (value instanceof ClassReferenceTreeNode node) {
				this.setIcon(GuiUtil.getClassIcon(this.gui, node.getEntry()));
			} else if (value instanceof MethodReferenceTreeNode node) {
				this.setIcon(GuiUtil.getMethodIcon(node.getEntry()));
			} else if (value instanceof FieldReferenceTreeNode) {
				this.setIcon(GuiUtil.FIELD_ICON);
			}
		}

		return c;
	}
}

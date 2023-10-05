package org.quiltmc.enigma.gui.renderer;

import org.quiltmc.enigma.analysis.ClassImplementationsTreeNode;
import org.quiltmc.enigma.analysis.MethodImplementationsTreeNode;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.UiConfig;
import org.quiltmc.enigma.gui.util.GuiUtil;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

public class ImplementationsTreeCellRenderer extends DefaultTreeCellRenderer {
	private final Gui gui;

	public ImplementationsTreeCellRenderer(Gui gui) {
		this.gui = gui;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		this.setForeground(UiConfig.getTextColor());

		if (value instanceof ClassImplementationsTreeNode node) {
			this.setIcon(GuiUtil.getClassIcon(this.gui, node.getClassEntry()));
		} else if (value instanceof MethodImplementationsTreeNode node) {
			this.setIcon(GuiUtil.getMethodIcon(node.getMethodEntry()));
		}

		return c;
	}
}

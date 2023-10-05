package org.quiltmc.enigma.gui.renderer;

import org.quiltmc.enigma.analysis.ClassInheritanceTreeNode;
import org.quiltmc.enigma.analysis.MethodInheritanceTreeNode;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.UiConfig;
import org.quiltmc.enigma.gui.util.GuiUtil;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class InheritanceTreeCellRenderer extends DefaultTreeCellRenderer {
	private final Gui gui;

	public InheritanceTreeCellRenderer(Gui gui) {
		this.gui = gui;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component ret = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		if (!(value instanceof MethodInheritanceTreeNode node) || node.isImplemented()) {
			ret.setForeground(UiConfig.getTextColor());
			ret.setFont(ret.getFont().deriveFont(Font.PLAIN));
			if (value instanceof ClassInheritanceTreeNode) {
				this.setIcon(GuiUtil.getClassIcon(this.gui, ((ClassInheritanceTreeNode) value).getClassEntry()));
			} else if (value instanceof MethodInheritanceTreeNode) {
				this.setIcon(GuiUtil.getMethodIcon(((MethodInheritanceTreeNode) value).getMethodEntry()));
			}
		} else {
			ret.setForeground(UiConfig.getNumberColor());
			ret.setFont(ret.getFont().deriveFont(Font.ITALIC));
			this.setIcon(GuiUtil.CLASS_ICON);
		}

		return ret;
	}
}

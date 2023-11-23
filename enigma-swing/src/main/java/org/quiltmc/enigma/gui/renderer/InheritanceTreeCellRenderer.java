package org.quiltmc.enigma.gui.renderer;

import org.quiltmc.enigma.api.analysis.tree.ClassInheritanceTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodInheritanceTreeNode;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
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
			ret.setForeground(Config.getCurrentColors().text.value());
			ret.setFont(ret.getFont().deriveFont(Font.PLAIN));
			if (value instanceof ClassInheritanceTreeNode) {
				this.setIcon(GuiUtil.getClassIcon(this.gui, ((ClassInheritanceTreeNode) value).getClassEntry()));
			} else if (value instanceof MethodInheritanceTreeNode) {
				this.setIcon(GuiUtil.getMethodIcon(((MethodInheritanceTreeNode) value).getMethodEntry()));
			}
		} else {
			ret.setForeground(Config.getCurrentColors().number.value());
			ret.setFont(ret.getFont().deriveFont(Font.ITALIC));
			this.setIcon(GuiUtil.CLASS_ICON);
		}

		return ret;
	}
}

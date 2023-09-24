package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.stats.ProjectStatsResult;
import cuchaz.enigma.stats.StatsResult;
import cuchaz.enigma.stats.StatType;
import cuchaz.enigma.stats.StatsGenerator;
import cuchaz.enigma.utils.I18n;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer {
	private final GuiController controller;
	private final ClassSelector selector;

	public ClassTreeCellRenderer(Gui gui, ClassSelector selector) {
		this.controller = gui.getController();
		this.selector = selector;

		// todo folder icons crash for some reason
		//this.setLeafIcon(null);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		if (this.controller.getProject() != null && leaf && value instanceof ClassSelectorClassNode node) {
			class TooltipPanel extends JPanel {
				@Override
				public String getToolTipText(MouseEvent event) {
					StringBuilder text = new StringBuilder(I18n.translateFormatted("class_selector.tooltip.stats_for", node.getDeobfEntry().getSimpleName()));
					text.append(System.lineSeparator());

					StatsGenerator generator = ClassTreeCellRenderer.this.controller.getStatsGenerator();

					if (generator == null || generator.getResultNullable() == null) {
						text.append(I18n.translate("class_selector.tooltip.stats_not_generated"));
					} else {
						StatsResult stats = ClassTreeCellRenderer.this.controller.getStatsGenerator().getStats(node.getObfEntry());

						if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
							for (int i = 0; i < StatType.values().length; i++) {
								StatType type = StatType.values()[i];
								text.append(type.getName()).append(": ").append(stats.toString(type)).append(i == StatType.values().length - 1 ? "" : "\n");
							}
						} else {
							text.append(stats);
						}
					}

					return text.toString();
				}
			}

			JPanel panel = new TooltipPanel();
			panel.setOpaque(false);
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JLabel nodeLabel = new JLabel(GuiUtil.getClassIcon(this.controller.getGui(), node.getObfEntry()));
			panel.add(nodeLabel);

			if (this.controller.getStatsGenerator() != null) {
				ProjectStatsResult stats = this.controller.getStatsGenerator().getResultNullable();
				if (stats == null) {
					// calculate stats on a separate thread for performance reasons
					this.setIcon(GuiUtil.PENDING_STATUS_ICON);
					node.reloadStats(this.controller.getGui(), this.selector, false);
				} else {
					this.setIcon(GuiUtil.getDeobfuscationIcon(stats, node.getObfEntry()));
				}
			} else {
				this.setIcon(GuiUtil.PENDING_STATUS_ICON);
			}

			panel.add(this);

			return panel;
		} else if (this.controller.getProject() != null && value instanceof ClassSelectorPackageNode node) {
			class TooltipPanel extends JPanel {
				@Override
				public String getToolTipText(MouseEvent event) {
					StringBuilder text = new StringBuilder(I18n.translateFormatted("class_selector.tooltip.stats_for", node.getPackageName()));
					text.append(System.lineSeparator());

					StatsGenerator generator = ClassTreeCellRenderer.this.controller.getStatsGenerator();

					if (generator == null || generator.getResultNullable() == null) {
						text.append(I18n.translate("class_selector.tooltip.stats_not_generated"));
					} else {
						StatsResult stats = ClassTreeCellRenderer.this.controller.getStatsGenerator().getResultNullable().getPackageStats(node.getPackageName());

						if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
							for (int i = 0; i < StatType.values().length; i++) {
								StatType type = StatType.values()[i];
								text.append(type.getName()).append(": ").append(stats.toString(type)).append(i == StatType.values().length - 1 ? "" : "\n");
							}
						} else {
							text.append(stats);
						}
					}

					return text.toString();
				}
			}

			JPanel panel = new TooltipPanel();
			panel.setOpaque(false);
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JLabel nodeLabel = new JLabel(GuiUtil.getFolderIcon(this, tree, node));
			panel.add(nodeLabel);

			if (this.controller.getStatsGenerator() != null) {
				ProjectStatsResult stats = this.controller.getStatsGenerator().getResultNullable();
				if (stats == null) {
					// calculate stats on a separate thread for performance reasons
					this.setIcon(GuiUtil.PENDING_STATUS_ICON);
					node.reloadStats(this.controller.getGui(), this.selector);
				} else {
					this.setIcon(GuiUtil.getDeobfuscationIcon(stats, node.getPackageName()));
				}
			} else {
				this.setIcon(GuiUtil.PENDING_STATUS_ICON);
			}

			panel.add(this);

			return panel;
		}

		return this;
	}
}

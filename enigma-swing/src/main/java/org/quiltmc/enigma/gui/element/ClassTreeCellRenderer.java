package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.stats.StatsResult;
import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.node.ClassSelectorClassNode;
import org.quiltmc.enigma.gui.node.ClassSelectorPackageNode;
import org.quiltmc.enigma.gui.util.GuiUtil;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.util.function.Function;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer {
	private final GuiController controller;
	private final ClassSelector selector;

	public ClassTreeCellRenderer(Gui gui, ClassSelector selector) {
		this.controller = gui.getController();
		this.selector = selector;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		if ((this.controller.getProject() != null && leaf && value instanceof ClassSelectorClassNode)
				|| (this.controller.getProject() != null && value instanceof ClassSelectorPackageNode)) {
			StatsTooltipPanel panel;
			Icon icon;
			Function<ProjectStatsResult, Icon> deobfuscationIconGetter;
			Runnable reloader;

			if (value instanceof ClassSelectorPackageNode node) {
				class PackageStatsTooltipPanel extends StatsTooltipPanel {
					PackageStatsTooltipPanel(GuiController controller) {
						super(controller);
					}

					@Override
					StatsResult getStats(StatsGenerator generator) {
						return generator.getResultNullable(Config.stats().createIconGenParameters(ClassTreeCellRenderer.this.controller.getGui().getEditableStatTypes())).getPackageStats(this.getDisplayName());
					}

					@Override
					String getDisplayName() {
						return node.getPackageName();
					}
				}

				panel = new PackageStatsTooltipPanel(this.controller);
				icon = GuiUtil.getFolderIcon(this, tree, node);
				deobfuscationIconGetter = projectStatsResult -> GuiUtil.getDeobfuscationIcon(projectStatsResult, node.getPackageName());
				reloader = () -> {};
			} else {
				ClassSelectorClassNode node = (ClassSelectorClassNode) value;

				class ClassStatsTooltipPanel extends StatsTooltipPanel {
					ClassStatsTooltipPanel(GuiController controller) {
						super(controller);
					}

					@Override
					StatsResult getStats(StatsGenerator generator) {
						return generator.getStats(node.getObfEntry());
					}

					@Override
					String getDisplayName() {
						return node.getDeobfEntry().getSimpleName();
					}
				}

				panel = new ClassStatsTooltipPanel(this.controller);
				icon = GuiUtil.getClassIcon(this.controller.getGui(), node.getObfEntry());
				deobfuscationIconGetter = projectStatsResult -> GuiUtil.getDeobfuscationIcon(projectStatsResult, node.getObfEntry());
				reloader = () -> node.reloadStats(this.controller.getGui(), this.selector, false);
			}

			panel.setOpaque(false);
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JLabel nodeLabel = new JLabel(icon);
			panel.add(nodeLabel);

			if (Config.main().features.enableClassTreeStatIcons.value()) {
				if (this.controller.getStatsGenerator() != null) {
					ProjectStatsResult stats = this.controller.getStatsGenerator().getResultNullable(Config.stats().createIconGenParameters(this.controller.getGui().getEditableStatTypes()));
					if (stats == null) {
						// calculate stats on a separate thread for performance reasons
						this.setIcon(GuiUtil.PENDING_STATUS_ICON);
						reloader.run();
					} else {
						this.setIcon(deobfuscationIconGetter.apply(stats));
					}
				} else {
					this.setIcon(GuiUtil.PENDING_STATUS_ICON);
				}
			} else {
				this.setIcon(null);
			}

			panel.add(this);

			return panel;
		}

		return this;
	}
}

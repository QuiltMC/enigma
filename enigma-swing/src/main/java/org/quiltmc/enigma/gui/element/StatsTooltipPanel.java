package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.stats.StatsResult;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JPanel;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public abstract class StatsTooltipPanel extends JPanel {
	private final GuiController controller;

	public StatsTooltipPanel(GuiController controller) {
		this.controller = controller;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		StringBuilder text = new StringBuilder(I18n.translateFormatted("class_selector.tooltip.stats_for", this.getDisplayName()));
		text.append(System.lineSeparator());

		StatsGenerator generator = this.controller.getStatsGenerator();

		if (generator == null || generator.getResultNullable() == null) {
			text.append(I18n.translate("class_selector.tooltip.stats_not_generated"));
		} else {
			StatsResult stats = this.getStats(generator);

			if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
				for (int i = 0; i < StatType.values().length; i++) {
					StatType type = StatType.values()[i];
					if (Config.stats().includedStatTypes.value().contains(type)) {
						text.append(type.getName()).append(": ").append(stats.toString(type)).append(i == StatType.values().length - 1 ? "" : "\n");
					}
				}
			} else {
				text.append(stats);
			}
		}

		return text.toString();
	}

	abstract StatsResult getStats(StatsGenerator generator);

	abstract String getDisplayName();
}

package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.stats.StatType;
import cuchaz.enigma.stats.StatsGenerator;
import cuchaz.enigma.stats.StatsResult;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public abstract class TooltipPanel extends JPanel {
	private final GuiController controller;

	public TooltipPanel(GuiController controller) {
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
					text.append(type.getName()).append(": ").append(stats.toString(type)).append(i == StatType.values().length - 1 ? "" : "\n");
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

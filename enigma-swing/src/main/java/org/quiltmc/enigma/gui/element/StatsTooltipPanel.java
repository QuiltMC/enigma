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
import java.util.ArrayList;

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
				var includedTypes = new ArrayList<>(Config.stats().getIncludedTypesForIcons(this.controller.getGui().getEditableStatTypes()));

				for (int i = 0; i < includedTypes.size(); i++) {
					StatType type = includedTypes.get(i);
					text
						.append(type.getName())
						.append(": ")
						.append(stats.toString(type))
						.append(i == includedTypes.size() - 1 ? "" : "\n");
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

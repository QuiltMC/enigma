package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import java.util.HashMap;
import java.util.Map;

public class StatsMenu extends AbstractEnigmaMenu {
	private final Gui gui;

	private final JCheckBoxMenuItem enableIcons = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem includeSynthetic = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem countFallback = new JCheckBoxMenuItem();
	private final JMenu statTypes = new JMenu();
	private final Map<StatType, JCheckBoxMenuItem> statTypeItems = new HashMap<>();

	public StatsMenu(Gui gui) {
		this.gui = gui;

		this.add(this.enableIcons);
		this.add(this.includeSynthetic);
		this.add(this.countFallback);
		this.add(this.statTypes);

		this.enableIcons.addActionListener(e -> this.onEnableIconsClicked());
		this.includeSynthetic.addActionListener(e -> this.onIncludeSyntheticClicked());
		this.countFallback.addActionListener(e -> this.onCountFallbackClicked());
		for (StatType statType : StatType.values()) {
			JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(statType.getName());
			checkbox.setSelected(Config.main().stats.includedStatTypes.value().contains(statType));
			checkbox.addActionListener(event -> {
				if (checkbox.isSelected() && !Config.stats().includedStatTypes.value().contains(statType)) {
					Config.stats().includedStatTypes.value().add(statType);
				} else {
					Config.stats().includedStatTypes.value().remove(statType);
				}

				this.gui.getController().regenerateAndUpdateStatIcons();
			});

			this.statTypeItems.put(statType, checkbox);
			this.statTypes.add(checkbox);
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.stat_icons"));

		this.enableIcons.setText(I18n.translate("menu.view.stat_icons.enable_icons"));
		this.includeSynthetic.setText(I18n.translate("menu.view.stat_icons.include_synthetic"));
		this.countFallback.setText(I18n.translate("menu.view.stat_icons.count_fallback"));
		this.statTypes.setText(I18n.translate("menu.view.stat_icons.included_types"));

		for (StatType statType : StatType.values()) {
			this.statTypeItems.get(statType).setText(statType.getName());
		}
	}

	@Override
	public void updateState() {
		this.enableIcons.setSelected(Config.main().features.enableClassTreeStatIcons.value());
		this.includeSynthetic.setSelected(Config.main().stats.shouldIncludeSyntheticParameters.value());
		this.countFallback.setSelected(Config.main().stats.shouldCountFallbackNames.value());
	}

	private void onEnableIconsClicked() {
		Config.main().features.enableClassTreeStatIcons.setValue(this.enableIcons.isSelected());
		this.gui.getController().regenerateAndUpdateStatIcons();
	}

	private void onIncludeSyntheticClicked() {
		Config.main().stats.shouldIncludeSyntheticParameters.setValue(this.includeSynthetic.isSelected());
		this.gui.getController().regenerateAndUpdateStatIcons();
	}

	private void onCountFallbackClicked() {
		Config.main().stats.shouldCountFallbackNames.setValue(this.countFallback.isSelected());
		this.gui.getController().regenerateAndUpdateStatIcons();
	}
}

package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleCheckBoxItem;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.CompletableFuture.runAsync;

public class StatsMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.stat_icons";

	private final SimpleCheckBoxItem enableIcons = new SimpleCheckBoxItem("menu.view.stat_icons.enable_icons");
	private final SimpleCheckBoxItem includeSynthetic = new SimpleCheckBoxItem("menu.view.stat_icons.include_synthetic");
	private final SimpleCheckBoxItem countFallback = new SimpleCheckBoxItem("menu.view.stat_icons.count_fallback");
	private final TypeMenu typeMenu = new TypeMenu();

	public StatsMenu(Gui gui) {
		super(gui);

		this.add(this.enableIcons);
		this.add(this.includeSynthetic);
		this.add(this.countFallback);
		this.add(this.typeMenu);

		this.enableIcons.addActionListener(e -> this.onEnableIconsClicked());
		this.includeSynthetic.addActionListener(e -> this.onIncludeSyntheticClicked());
		this.countFallback.addActionListener(e -> this.onCountFallbackClicked());
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));

		this.enableIcons.retranslate();
		this.includeSynthetic.retranslate();
		this.countFallback.retranslate();
		this.typeMenu.retranslate();
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.enableIcons.setSelected(Config.stats().enableClassTreeStatIcons.value());
		this.includeSynthetic.setSelected(Config.main().stats.shouldIncludeSyntheticParameters.value());
		this.countFallback.setSelected(Config.main().stats.shouldCountFallbackNames.value());

		this.typeMenu.updateState(jarOpen, state);
	}

	private void onEnableIconsClicked() {
		Config.stats().enableClassTreeStatIcons.setValue(this.enableIcons.isSelected());
		this.updateIconsLater();
	}

	private void onIncludeSyntheticClicked() {
		Config.main().stats.shouldIncludeSyntheticParameters.setValue(this.includeSynthetic.isSelected());
		this.updateIconsLater();
	}

	private void onCountFallbackClicked() {
		Config.main().stats.shouldCountFallbackNames.setValue(this.countFallback.isSelected());
		this.updateIconsLater();
	}

	private void updateIconsLater() {
		SwingUtilities.invokeLater(() -> runAsync(() -> this.gui.getController().regenerateAndUpdateStatIcons()));
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}

	private final class TypeMenu extends AbstractSearchableEnigmaMenu {
		static final String TRANSLATION_KEY = "menu.view.stat_icons.included_types";

		private final Map<StatType, SimpleCheckBoxItem> items = new HashMap<>();

		TypeMenu() {
			super(StatsMenu.this.gui);

			for (StatType statType : StatType.values()) {
				SimpleCheckBoxItem checkbox = new SimpleCheckBoxItem(statType.getTranslationKey());
				checkbox.addActionListener(event -> this.onTypeClicked(statType));

				this.items.put(statType, checkbox);
				this.add(checkbox);
			}
		}

		@Override
		public String getAliasesTranslationKeyPrefix() {
			return TRANSLATION_KEY;
		}

		@Override
		public void retranslate() {
			this.setText(I18n.translate(TRANSLATION_KEY));

			this.items.values().forEach(SimpleCheckBoxItem::retranslate);
		}

		@Override
		public void updateState(boolean jarOpen, ConnectionState state) {
			for (StatType type : StatType.values()) {
				JCheckBoxMenuItem checkbox = this.items.get(type);
				checkbox.setSelected(Config.main().stats.includedStatTypes.value().contains(type));
			}
		}

		void onTypeClicked(StatType type) {
			JCheckBoxMenuItem checkbox = this.items.get(type);

			if (checkbox.isSelected() && !Config.stats().includedStatTypes.value().contains(type)) {
				Config.stats().includedStatTypes.value().add(type);
			} else {
				Config.stats().includedStatTypes.value().remove(type);
			}

			StatsMenu.this.updateIconsLater();
		}
	}
}

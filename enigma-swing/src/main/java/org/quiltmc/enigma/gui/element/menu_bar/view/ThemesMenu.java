package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ChangeDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.ConventionalSearchableElement;
import org.quiltmc.enigma.gui.element.menu_bar.Retranslatable;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.quiltmc.enigma.gui.config.Config.ThemeChoice;

public class ThemesMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.themes";

	private final Map<ThemeChoice, ThemeItem> themes = new HashMap<>();

	protected ThemesMenu(Gui gui) {
		super(gui);

		ButtonGroup themeGroup = new ButtonGroup();
		for (ThemeChoice theme : ThemeChoice.values()) {
			ThemeItem themeItem = new ThemeItem(theme);
			themeGroup.add(themeItem);
			this.themes.put(theme, themeItem);

			this.add(themeItem);
			themeItem.addActionListener(e -> this.onThemeClicked(theme));
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));

		this.themes.values().forEach(ThemeItem::retranslate);
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		for (ThemeChoice theme : ThemeChoice.values()) {
			if (theme.equals(Config.main().theme.value())) {
				this.themes.get(theme).setSelected(true);
			}
		}
	}

	private void onThemeClicked(ThemeChoice theme) {
		Config.main().theme.setValue(theme, true);
		ChangeDialog.show(this.gui.getFrame());
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}

	private static final class ThemeItem extends JRadioButtonMenuItem implements ConventionalSearchableElement, Retranslatable {
		final ThemeChoice theme;
		final String translationKey;

		private ThemeItem(ThemeChoice theme) {
			this.theme = theme;
			this.translationKey = ThemesMenu.TRANSLATION_KEY + "." + theme.name().toLowerCase(Locale.ROOT);
		}

		@Override
		public void retranslate() {
			this.setText(I18n.translate(this.translationKey));
		}

		@Override
		public String getSearchName() {
			return this.getText();
		}

		@Override
		public String getAliasesTranslationKeyPrefix() {
			return this.translationKey;
		}

		@Override
		public void onSearchChosen() {
			this.doClick(0);
		}
	}
}

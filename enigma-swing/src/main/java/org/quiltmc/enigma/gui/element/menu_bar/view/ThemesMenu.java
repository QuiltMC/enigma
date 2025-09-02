package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ChangeDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.quiltmc.enigma.gui.config.Config.ThemeChoice;

public class ThemesMenu extends AbstractEnigmaMenu {
	private final Map<ThemeChoice, JRadioButtonMenuItem> themes = new HashMap<>();

	protected ThemesMenu(Gui gui) {
		super(gui);

		ButtonGroup themeGroup = new ButtonGroup();
		for (ThemeChoice theme : ThemeChoice.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem();
			themeGroup.add(themeButton);
			this.themes.put(theme, themeButton);

			this.add(themeButton);
			themeButton.addActionListener(e -> this.onThemeClicked(theme));
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.themes"));

		for (ThemeChoice theme : ThemeChoice.values()) {
			this.themes.get(theme).setText(I18n.translate("menu.view.themes." + theme.name().toLowerCase(Locale.ROOT)));
		}
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
}

package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.FontDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenuItem;

public class ViewMenu extends AbstractEnigmaMenu {
	private final StatsMenu stats;
	private final NotificationsMenu notifications;
	private final LanguagesMenu languages;
	private final ThemesMenu themes;
	private final ScaleMenu scale;

	private final JMenuItem fontItem = new JMenuItem();

	public ViewMenu(Gui gui) {
		super(gui);
		this.stats = new StatsMenu(gui);
		this.notifications = new NotificationsMenu(gui);
		this.languages = new LanguagesMenu(gui);
		this.themes = new ThemesMenu(gui);
		this.scale = new ScaleMenu(gui);

		this.add(this.themes);
		this.add(this.languages);
		this.add(this.notifications);
		this.add(this.scale);
		this.add(this.stats);
		this.add(this.fontItem);

		this.fontItem.addActionListener(e -> this.onFontClicked(this.gui));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view"));

		this.themes.retranslate();
		this.notifications.retranslate();
		this.languages.retranslate();
		this.scale.retranslate();
		this.stats.retranslate();
		this.fontItem.setText(I18n.translate("menu.view.font"));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.stats.updateState(jarOpen, state);
		this.notifications.updateState(jarOpen, state);
		this.languages.updateState(jarOpen, state);
		this.scale.updateState(jarOpen, state);
	}

	private void onFontClicked(Gui gui) {
		FontDialog.display(gui.getFrame());
	}
}

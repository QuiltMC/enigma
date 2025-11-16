package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Map;

import static org.quiltmc.enigma.gui.NotificationManager.ServerNotificationLevel;

public class NotificationsMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.notifications";

	private final Map<ServerNotificationLevel, JRadioButtonMenuItem> buttons = new HashMap<>();

	public NotificationsMenu(Gui gui) {
		super(gui);

		ButtonGroup buttonGroup = new ButtonGroup();
		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			JRadioButtonMenuItem notificationsButton = new JRadioButtonMenuItem();
			buttonGroup.add(notificationsButton);
			this.buttons.put(level, notificationsButton);
			notificationsButton.addActionListener(event -> Config.main().serverNotificationLevel.setValue(level, true));
			this.add(notificationsButton);
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));

		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			this.buttons.get(level).setText(level.getText());
		}
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			this.buttons.get(level).setSelected(level.equals(Config.main().serverNotificationLevel.value()));
		}
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

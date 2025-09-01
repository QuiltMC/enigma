package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Map;

import static org.quiltmc.enigma.gui.NotificationManager.ServerNotificationLevel;

public class NotificationsMenu extends AbstractEnigmaMenu {
	private final Map<ServerNotificationLevel, JRadioButtonMenuItem> buttons = new HashMap<>();

	public NotificationsMenu(Gui gui) {
		super(gui);

		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			JRadioButtonMenuItem notificationsButton = new JRadioButtonMenuItem();
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(notificationsButton);
			this.buttons.put(level, notificationsButton);
			notificationsButton.addActionListener(event -> Config.main().serverNotificationLevel.setValue(level, true));
			this.add(notificationsButton);
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.notifications"));

		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			this.buttons.get(level).setText(level.getText());
		}
	}

	@Override
	public void updateState() {
		for (ServerNotificationLevel level : ServerNotificationLevel.values()) {
			this.buttons.get(level).setSelected(level.equals(Config.main().serverNotificationLevel.value()));
		}
	}
}

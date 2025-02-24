package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.NotificationManager;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ConnectToServerDialog;
import org.quiltmc.enigma.gui.dialog.CreateServerDialog;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.util.Arrays;

public class CollabMenu extends AbstractEnigmaMenu {
	private final Gui gui;

	private final JMenuItem connectItem = new JMenuItem();
	private final JMenuItem startServerItem = new JMenuItem();

	public CollabMenu(Gui gui) {
		this.gui = gui;

		this.add(this.connectItem);
		this.add(this.startServerItem);

		this.connectItem.addActionListener(e -> this.onConnectClicked());
		this.startServerItem.addActionListener(e -> this.onStartServerClicked());
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.collab"));
		this.connectItem.setText(I18n.translate("menu.collab.connect"));
		this.startServerItem.setText(I18n.translate("menu.collab.server.start"));
	}

	@Override
	public void updateState() {
		boolean jarOpen = this.gui.isJarOpen();
		ConnectionState connectionState = this.gui.getConnectionState();

		this.connectItem.setEnabled(jarOpen && connectionState != ConnectionState.HOSTING);
		this.connectItem.setText(I18n.translate(connectionState != ConnectionState.CONNECTED ? "menu.collab.connect" : "menu.collab.disconnect"));
		this.startServerItem.setEnabled(jarOpen && connectionState != ConnectionState.CONNECTED);
		this.startServerItem.setText(I18n.translate(connectionState != ConnectionState.HOSTING ? "menu.collab.server.start" : "menu.collab.server.stop"));

	}

	public void onConnectClicked() {
		if (this.gui.getController().getClient() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}

		ConnectToServerDialog.Result result = ConnectToServerDialog.show(this.gui);
		if (result == null) {
			return;
		}

		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createClient(result.username(), result.address().address, result.address().port, result.password());
			if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.CONNECTED_TO_SERVER, result.addressStr()));
			}

			Config.net().username.setValue(result.username(), true);
			Config.net().remoteAddress.setValue(result.addressStr(), true);
			Config.net().password.setValue(String.valueOf(result.password()), true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.connect.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}

		Arrays.fill(result.password(), (char) 0);
	}

	public void onStartServerClicked() {
		if (this.gui.getController().getServer() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}

		CreateServerDialog.Result result = CreateServerDialog.show(this.gui);
		if (result == null) {
			return;
		}

		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createServer(result.username(), result.port(), result.password());
			if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.SERVER_STARTED, result.port()));
			}

			Config.net().username.setValue(result.username(), true);
			Config.net().serverPort.setValue(result.port(), true);
			Config.net().serverPassword.setValue(String.valueOf(result.password()), true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.server.start.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
	}
}

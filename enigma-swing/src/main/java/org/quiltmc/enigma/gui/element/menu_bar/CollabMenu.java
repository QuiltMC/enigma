package org.quiltmc.enigma.gui.element.menu_bar;

import org.jspecify.annotations.Nullable;
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
import java.util.function.Function;
import java.util.stream.Stream;

public class CollabMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.collab";

	private final StatefulItem connectionItem = new StatefulItem(state -> state != ConnectionState.CONNECTED
			? "menu.collab.connect"
			: "menu.collab.disconnect"
	);

	private final StatefulItem hostItem = new StatefulItem(state -> state != ConnectionState.HOSTING
			? "menu.collab.server.start"
			: "menu.collab.server.stop"
	);

	public CollabMenu(Gui gui) {
		super(gui);

		this.add(this.connectionItem);
		this.add(this.hostItem);

		this.connectionItem.addActionListener(e -> this.onConnectionClicked());
		this.hostItem.addActionListener(e -> this.onHostClicked());
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.retranslate(this.gui.getConnectionState());
	}

	private void retranslate(ConnectionState state) {
		this.connectionItem.retranslate(state);
		this.hostItem.retranslate(state);
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.connectionItem.setEnabled(jarOpen && state != ConnectionState.HOSTING);
		this.hostItem.setEnabled(jarOpen && state != ConnectionState.CONNECTED);
		this.retranslate(state);
	}

	public void onConnectionClicked() {
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

	public void onHostClicked() {
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

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}

	private static final class StatefulItem extends JMenuItem implements SearchableElement {
		final Function<ConnectionState, String> updateTranslationKey;

		@Nullable
		String translationKey;

		StatefulItem(Function<ConnectionState, String> updateTranslationKey) {
			this.updateTranslationKey = updateTranslationKey;
		}

		@Override
		public Stream<String> streamSearchAliases() {
			return this.translationKey == null ? Stream.empty() : Stream.concat(
				Stream.of(this.getSearchName()),
				SearchableElement.translateExtraAliases(this.translationKey)
			);
		}

		void retranslate(ConnectionState state) {
			this.translationKey = this.updateTranslationKey.apply(state);

			this.setText(I18n.translate(this.translationKey));
		}

		@Override
		public String getSearchName() {
			return this.getText();
		}

		@Override
		public void onSearchClicked() {
			this.doClick();
		}
	}
}

package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JPasswordField;
import javax.swing.JTextField;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.network.ServerAddress;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

public class ConnectToServerDialog extends AbstractDialog {

	private JTextField usernameField;
	private JTextField ipField;
	private JPasswordField passwordField;

	public ConnectToServerDialog(Frame owner, Gui gui) {
		super(owner, gui, "prompt.connect.title", "prompt.connect.confirm", "prompt.cancel");

		Dimension preferredSize = getPreferredSize();
		preferredSize.width = ScaleUtil.scale(400);
		setPreferredSize(preferredSize);
		pack();
		setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		usernameField = new JTextField(NetConfig.getUsername());
		ipField = new JTextField(NetConfig.getRemoteAddress());
		passwordField = new JPasswordField(NetConfig.getPassword());

		usernameField.addActionListener(event -> confirm());
		ipField.addActionListener(event -> confirm());
		passwordField.addActionListener(event -> confirm());

		return Arrays.asList(
				new Pair<>("prompt.connect.username", usernameField),
				new Pair<>("prompt.connect.address", ipField),
				new Pair<>("prompt.password", passwordField)
		);
	}

	@Override
	public void validateInputs() {
		if (StandardValidation.notBlank(vc, ipField.getText())) {
			if (ServerAddress.from(ipField.getText(), EnigmaServer.DEFAULT_PORT) == null) {
				vc.raise(Message.INVALID_IP);
			}
		}
	}

	public Result getResult() {
		if (!isActionConfirm()) return null;
		vc.reset();
		validateInputs();
		if (!vc.canProceed()) return null;
		return new Result(
				usernameField.getText(),
				ipField.getText(),
				Objects.requireNonNull(ServerAddress.from(ipField.getText(), EnigmaServer.DEFAULT_PORT)),
				passwordField.getPassword()
		);
	}

	public static Result show(Gui gui) {
		ConnectToServerDialog d = new ConnectToServerDialog(gui.getFrame(), gui);

		d.setVisible(true);
		Result r = d.getResult();

		d.dispose();
		return r;
	}

	public static class Result {
		private final String username;
		private final String addressStr;
		private final ServerAddress address;
		private final char[] password;

		public Result(String username, String addressStr, ServerAddress address, char[] password) {
			this.username = username;
			this.addressStr = addressStr;
			this.address = address;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getAddressStr() {
			return addressStr;
		}

		public ServerAddress getAddress() {
			return address;
		}

		public char[] getPassword() {
			return password;
		}
	}

}

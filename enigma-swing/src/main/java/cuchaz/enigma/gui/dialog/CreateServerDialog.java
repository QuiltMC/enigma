package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CreateServerDialog extends AbstractDialog {

	private JTextField portField;
	private JPasswordField passwordField;

	public CreateServerDialog(Frame owner, Gui gui) {
		super(owner, gui, "prompt.create_server.title", "prompt.create_server.confirm", "prompt.cancel");

		Dimension preferredSize = getPreferredSize();
		preferredSize.width = ScaleUtil.scale(400);
		setPreferredSize(preferredSize);
		pack();
		setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		portField = new JTextField(Integer.toString(NetConfig.getServerPort()));
		passwordField = new JPasswordField(NetConfig.getServerPassword());

		portField.addActionListener(event -> confirm());
		passwordField.addActionListener(event -> confirm());

		return Arrays.asList(
				new Pair<>("prompt.create_server.port", portField),
				new Pair<>("prompt.password", passwordField)
		);
	}

	@Override
	public void validateInputs() {
		StandardValidation.isIntInRange(vc, portField.getText(), 0, 65535);
		if (passwordField.getPassword().length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			vc.raise(Message.FIELD_LENGTH_OUT_OF_RANGE, EnigmaServer.MAX_PASSWORD_LENGTH);
		}
	}

	public Result getResult() {
		if (!isActionConfirm()) return null;
		vc.reset();
		validateInputs();
		if (!vc.canProceed()) return null;
		return new Result(
				Integer.parseInt(portField.getText()),
				passwordField.getPassword()
		);
	}

	public static Result show(Gui gui) {
		CreateServerDialog d = new CreateServerDialog(gui.getFrame(), gui);

		d.setVisible(true);
		Result r = d.getResult();

		d.dispose();
		return r;
	}

	public static class Result {
		private final int port;
		private final char[] password;

		public Result(int port, char[] password) {
			this.port = port;
			this.password = password;
		}

		public int getPort() {
			return port;
		}

		public char[] getPassword() {
			return password;
		}
	}

}

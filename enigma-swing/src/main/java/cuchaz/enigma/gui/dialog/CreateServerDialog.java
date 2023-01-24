package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;

import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.elements.ValidatablePasswordField;
import cuchaz.enigma.gui.elements.ValidatableTextField;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

public class CreateServerDialog extends AbstractDialog {

	private ValidatableTextField portField;
	private ValidatablePasswordField passwordField;

	public CreateServerDialog(Frame owner) {
		super(owner, "prompt.create_server.title", "prompt.create_server.confirm", "prompt.cancel");

		Dimension preferredSize = this.getPreferredSize();
		preferredSize.width = ScaleUtil.scale(400);
		this.setPreferredSize(preferredSize);
		this.pack();
		this.setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		this.portField = new ValidatableTextField(Integer.toString(NetConfig.getServerPort()));
		this.passwordField = new ValidatablePasswordField(NetConfig.getServerPassword());

		this.portField.addActionListener(event -> this.confirm());
		this.passwordField.addActionListener(event -> this.confirm());

		return Arrays.asList(
				new Pair<>("prompt.create_server.port", this.portField),
				new Pair<>("prompt.password", this.passwordField)
		);
	}

	@Override
	public void validateInputs() {
		this.vc.setActiveElement(this.portField);
		StandardValidation.isIntInRange(this.vc, this.portField.getText(), 0, 65535);
		this.vc.setActiveElement(this.passwordField);
		if (this.passwordField.getPassword().length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			this.vc.raise(Message.FIELD_LENGTH_OUT_OF_RANGE, EnigmaServer.MAX_PASSWORD_LENGTH);
		}
	}

	public Result getResult() {
		if (!this.isActionConfirm()) return null;
		this.vc.reset();
		this.validateInputs();
		if (!this.vc.canProceed()) return null;
		return new Result(
				Integer.parseInt(this.portField.getText()),
				this.passwordField.getPassword()
		);
	}

	public static Result show(Frame parent) {
		CreateServerDialog d = new CreateServerDialog(parent);

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
			return this.port;
		}

		public char[] getPassword() {
			return this.password;
		}
	}

}

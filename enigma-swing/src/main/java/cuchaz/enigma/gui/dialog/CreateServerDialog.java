package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CreateServerDialog extends AbstractDialog {
	private JTextField portField;
	private JPasswordField passwordField;

	public CreateServerDialog(Frame owner, Gui gui) {
		super(owner, gui, "prompt.create_server.title", "prompt.create_server.confirm", "prompt.cancel");

		Dimension preferredSize = this.getPreferredSize();
		preferredSize.width = ScaleUtil.scale(400);
		this.setPreferredSize(preferredSize);
		this.pack();
		this.setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		this.portField = new JTextField(Integer.toString(NetConfig.getServerPort()));
		this.passwordField = new JPasswordField(NetConfig.getServerPassword());

		this.portField.addActionListener(event -> this.confirm());
		this.passwordField.addActionListener(event -> this.confirm());

		return Arrays.asList(
				new Pair<>("prompt.create_server.port", this.portField),
				new Pair<>("prompt.password", this.passwordField)
		);
	}

	@Override
	public void validateInputs() {
		StandardValidation.isIntInRange(this.vc, this.portField.getText(), 0, 65535);
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

	public static Result show(Gui gui) {
		CreateServerDialog d = new CreateServerDialog(gui.getFrame(), gui);

		d.setVisible(true);
		Result r = d.getResult();

		d.dispose();
		return r;
	}

	public record Result(int port, char[] password) {
	}
}

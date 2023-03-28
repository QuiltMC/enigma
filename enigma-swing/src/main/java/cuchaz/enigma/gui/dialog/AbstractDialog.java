package cuchaz.enigma.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.ValidationContext;

public abstract class AbstractDialog extends JDialog {
	protected final ValidationContext vc;
	private boolean actionConfirm;

	protected AbstractDialog(Frame owner, Gui gui, String title, String confirmAction, String cancelAction) {
		super(owner, I18n.translate(title), true);

		this.vc = new ValidationContext(gui.getNotificationManager());
		this.actionConfirm = false;

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		Container inputContainer = new JPanel(new GridBagLayout());
		List<Pair<String, Component>> components = this.createComponents();

		for (int i = 0; i < components.size(); i += 1) {
			Pair<String, Component> entry = components.get(i);
			JLabel label = new JLabel(I18n.translate(entry.a()));
			Component component = entry.b();

			GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

			inputContainer.add(label, cb.pos(0, i).weightX(0.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
			inputContainer.add(component, cb.pos(1, i).weightX(1.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
		}

		contentPane.add(inputContainer, BorderLayout.CENTER);
		Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
		JButton connectButton = new JButton(I18n.translate(confirmAction));
		connectButton.addActionListener(event -> this.confirm());
		buttonContainer.add(connectButton);
		JButton abortButton = new JButton(I18n.translate(cancelAction));
		abortButton.addActionListener(event -> this.cancel());
		buttonContainer.add(abortButton);
		contentPane.add(buttonContainer, BorderLayout.SOUTH);

		this.pack();
		this.setLocationRelativeTo(owner);
	}

	protected abstract List<Pair<String, Component>> createComponents();

	protected void confirm() {
		this.vc.reset();
		this.validateInputs();
		if (this.vc.canProceed()) {
			this.actionConfirm = true;
			this.setVisible(false);
		}
	}

	protected void cancel() {
		this.actionConfirm = false;
		this.setVisible(false);
	}

	public boolean isActionConfirm() {
		return this.actionConfirm;
	}

	public void validateInputs() {
	}
}

package cuchaz.enigma.gui.elements;

import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.Document;

import com.formdev.flatlaf.FlatClientProperties;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.events.ConvertingTextFieldListener;
import cuchaz.enigma.gui.util.GuiUtil;

/**
 * A label that converts into an editable text field when you click it.
 */
public class ConvertingTextField {
	private final JPanel ui;
	private final JTextField textField;
	private final JLabel label;
	private boolean editing = false;
	private boolean editable = true;

	private final Set<ConvertingTextFieldListener> listeners = new HashSet<>();

	public ConvertingTextField(String text) {
		this.ui = new JPanel();
		this.ui.setLayout(new GridLayout(1, 1, 0, 0));
		this.textField = new JTextField(text);
		this.textField.putClientProperty(FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY_NEVER);
		this.label = GuiUtil.unboldLabel(new JLabel(text));
		this.label.setBorder(BorderFactory.createLoweredBevelBorder());

		this.label.addMouseListener(GuiUtil.onMouseClick(e -> this.startEditing()));

		this.textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (!ConvertingTextField.this.hasChanges()) {
					ConvertingTextField.this.stopEditing(true);
				}
			}
		});

		this.textField.addKeyListener(GuiUtil.onKeyPress(e -> {
			if (KeyBinds.EXIT.matches(e)) {
				this.stopEditing(true);
			} else if (KeyBinds.DIALOG_SAVE.matches(e)) {
				this.stopEditing(false);
			}
		}));

		this.ui.add(this.label);
	}

	public void startEditing() {
		if (this.editing || !this.editable) return;

		this.ui.removeAll();
		this.ui.add(this.textField);
		this.editing = true;
		this.ui.validate();
		this.ui.repaint();
		this.textField.requestFocusInWindow();
		this.textField.selectAll();
		this.listeners.forEach(l -> l.onStartEditing(this));
	}

	public void stopEditing(boolean abort) {
		if (!this.editing) return;

		if (!this.listeners.stream().allMatch(l -> l.tryStopEditing(this, abort))) return;

		if (abort) {
			this.textField.setText(this.label.getText());
		} else {
			this.label.setText(this.textField.getText());
		}

		this.ui.removeAll();
		this.ui.add(this.label);
		this.editing = false;
		this.ui.validate();
		this.ui.repaint();
		this.listeners.forEach(l -> l.onStopEditing(this, abort));
	}

	public void setText(String text) {
		this.stopEditing(true);
		this.label.setText(text);
		this.textField.setText(text);
	}

	/**
	 * Sets the visible text without aborting.
	 * <p>
	 * The text in the {@link #textField} is not replaced if it has been edited,
	 * allowing the user to continue editing their entry without interference.
	 *
	 * @param text The text to set the label and text field to.
	 */
	public void setReferenceText(String text) {
		if (!this.hasChanges()) {
			this.textField.setText(text);
		}

		this.label.setText(text);
	}

	public void setEditText(String text) {
		if (!this.editing) return;

		this.textField.setText(text);
	}

	public void setEditable(boolean editable) {
		if (!editable) {
			this.stopEditing(true);
		}

		this.label.setEnabled(editable);
		this.editable = editable;
	}

	public void selectAll() {
		if (!this.editing) return;

		this.textField.selectAll();
	}

	public void selectSubstring(int startIndex) {
		if (!this.editing) return;

		Document doc = this.textField.getDocument();
		if (doc != null) {
			this.selectSubstring(startIndex, doc.getLength());
		}
	}

	public void selectSubstring(int startIndex, int endIndex) {
		if (!this.editing) return;

		this.textField.select(startIndex, endIndex);
	}

	public String getText() {
		if (this.editing) {
			return this.textField.getText();
		} else {
			return this.label.getText();
		}
	}

	public String getPersistentText() {
		return this.label.getText();
	}

	public boolean hasChanges() {
		if (!this.editing) return false;
		return !this.textField.getText().equals(this.label.getText());
	}

	public void addListener(ConvertingTextFieldListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ConvertingTextFieldListener listener) {
		this.listeners.remove(listener);
	}

	public JPanel getUi() {
		return this.ui;
	}
}

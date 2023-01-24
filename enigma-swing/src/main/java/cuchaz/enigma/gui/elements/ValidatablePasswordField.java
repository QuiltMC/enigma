package cuchaz.enigma.gui.elements;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPasswordField;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.Validatable;

public class ValidatablePasswordField extends JPasswordField implements Validatable {

	private final List<ParameterizedMessage> messages = new ArrayList<>();
	private String tooltipText = null;

	public ValidatablePasswordField() {
	}

	public ValidatablePasswordField(String text) {
		super(text);
	}

	public ValidatablePasswordField(int columns) {
		super(columns);
	}

	public ValidatablePasswordField(String text, int columns) {
		super(text, columns);
	}

	public ValidatablePasswordField(Document doc, String txt, int columns) {
		super(doc, txt, columns);
	}

	{
		this.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				ValidatablePasswordField.this.clearMessages();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				ValidatablePasswordField.this.clearMessages();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				ValidatablePasswordField.this.clearMessages();
			}
		});
	}

	@Override
	public JToolTip createToolTip() {
		JMultiLineToolTip tooltip = new JMultiLineToolTip();
		tooltip.setComponent(this);
		return tooltip;
	}

	@Override
	public void setToolTipText(String text) {
		this.tooltipText = text;
		this.setToolTipText0();
	}

	private void setToolTipText0() {
		super.setToolTipText(ValidatableUi.getTooltipText(this.tooltipText, this.messages));
	}

	@Override
	public void clearMessages() {
		this.messages.clear();
		this.setToolTipText0();
		this.repaint();
	}

	@Override
	public void addMessage(ParameterizedMessage message) {
		this.messages.add(message);
		this.setToolTipText0();
		this.repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		ValidatableUi.drawMarker(this, g, this.messages);
	}

}

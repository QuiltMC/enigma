package cuchaz.enigma.gui.elements;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.Validatable;

public class ValidatableTextField extends JTextField implements Validatable {

	private final List<ParameterizedMessage> messages = new ArrayList<>();
	private String tooltipText = null;

	public ValidatableTextField() {
	}

	public ValidatableTextField(String text) {
		super(text);
	}

	public ValidatableTextField(int columns) {
		super(columns);
	}

	public ValidatableTextField(String text, int columns) {
		super(text, columns);
	}

	public ValidatableTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
	}

	{
		this.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				ValidatableTextField.this.clearMessages();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				ValidatableTextField.this.clearMessages();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				ValidatableTextField.this.clearMessages();
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

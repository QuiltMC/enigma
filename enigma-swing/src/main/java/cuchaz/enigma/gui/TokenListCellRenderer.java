package cuchaz.enigma.gui;

import cuchaz.enigma.source.Token;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;

public class TokenListCellRenderer implements ListCellRenderer<Token> {
	private final GuiController controller;
	private final DefaultListCellRenderer defaultRenderer;

	public TokenListCellRenderer(GuiController controller) {
		this.controller = controller;
		this.defaultRenderer = new DefaultListCellRenderer();
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Token> list, Token token, int index, boolean isSelected, boolean hasFocus) {
		JLabel label = (JLabel) this.defaultRenderer.getListCellRendererComponent(list, token, index, isSelected, hasFocus);
		label.setText(this.controller.getReadableToken(token).toString());
		return label;
	}
}

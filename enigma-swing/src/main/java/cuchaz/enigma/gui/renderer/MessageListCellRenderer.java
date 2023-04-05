package cuchaz.enigma.gui.renderer;

import cuchaz.enigma.network.ServerMessage;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

// For now, just render the translated text.
// TODO: Icons or something later?
public class MessageListCellRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		ServerMessage message = (ServerMessage) value;
		if (message != null) {
			this.setText(message.translate());
		}

		return this;
	}
}

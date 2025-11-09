package org.quiltmc.enigma.gui.util;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimpleAction extends AbstractAction {
	private final ActionListener listener;

	public SimpleAction(ActionListener listener) {
		this.listener = listener;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.listener.actionPerformed(e);
	}
}

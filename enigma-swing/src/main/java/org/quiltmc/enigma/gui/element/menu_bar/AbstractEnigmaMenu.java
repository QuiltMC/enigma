package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;

import javax.swing.JMenu;

public abstract class AbstractEnigmaMenu extends JMenu implements EnigmaMenu {
	protected final Gui gui;

	protected AbstractEnigmaMenu(Gui gui) {
		this.gui = gui;
	}

	public void updateState() {
		this.updateState(this.gui.isJarOpen(), this.gui.getConnectionState());
	}
}

package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;

import javax.swing.JMenu;

public class AbstractEnigmaMenu extends JMenu implements EnigmaMenu {
	protected final Gui gui;

	protected AbstractEnigmaMenu(Gui gui) {
		this.gui = gui;
	}
}

package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;

import javax.swing.MenuElement;

public interface EnigmaMenu extends MenuElement {
	default void setKeyBinds() { }

	default void updateState(boolean jarOpen, ConnectionState state) { }

	default void retranslate() { }
}

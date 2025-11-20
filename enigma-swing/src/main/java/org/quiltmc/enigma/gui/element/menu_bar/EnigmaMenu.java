package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;

import javax.swing.MenuElement;

public interface EnigmaMenu extends MenuElement, Retranslatable {
	default void setKeyBinds() { }

	default void updateState(boolean jarOpen, ConnectionState state) { }

	@Override
	default void retranslate() { }
}

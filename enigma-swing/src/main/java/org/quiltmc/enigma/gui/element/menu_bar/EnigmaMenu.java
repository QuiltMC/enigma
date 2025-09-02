package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;

public interface EnigmaMenu {
	default void setKeyBinds() {}

	default void updateState(boolean jarOpen, ConnectionState state) {}

	default void retranslate() {}
}

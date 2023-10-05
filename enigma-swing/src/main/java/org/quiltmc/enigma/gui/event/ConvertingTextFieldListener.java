package org.quiltmc.enigma.gui.event;

import org.quiltmc.enigma.gui.element.ConvertingTextField;

public interface ConvertingTextFieldListener {
	default void onStartEditing(ConvertingTextField field) {
	}

	default boolean tryStopEditing(ConvertingTextField field, boolean abort) {
		return true;
	}

	default void onStopEditing(ConvertingTextField field, boolean abort) {
	}
}

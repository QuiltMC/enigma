package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.config.keybind.KeyBind;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionListener;

public final class InputUtil {
	private InputUtil() {
		throw new UnsupportedOperationException();
	}

	public static void putKeybindAction(KeyBind keyBind, JComponent component, ActionListener listener) {
		putKeybindAction(keyBind, component, FocusCondition.WHEN_IN_FOCUSED_WINDOW, listener);
	}

	public static void putKeybindAction(
			KeyBind keyBind, JComponent component, FocusCondition condition, ActionListener listener
	) {
		putKeybindAction(keyBind, component, condition, new SimpleAction(listener));
	}

	public static void putKeybindAction(
			KeyBind keyBind, JComponent component, FocusCondition condition, Action action
	) {
		final InputMap inputMap = component.getInputMap(condition.value);

		if (inputMap != null) {
			final String actionKey = keyBind.name();

			final KeyStroke[] keys = inputMap.keys();
			if (keys != null) {
				for (final KeyStroke key : keys) {
					final Object value = inputMap.get(key);
					if (actionKey.equals(value)) {
						// remove previous bindings to action
						inputMap.remove(key);
					}
				}
			}

			keyBind.combinations().stream().map(combo -> combo.toKeyStroke(0))
					.forEach(key -> inputMap.put(key, actionKey));

			final ActionMap actionMap = component.getActionMap();
			if (actionMap != null) {
				actionMap.remove(actionKey);
				actionMap.put(actionKey, action);
			}
		}
	}

	public enum FocusCondition {
		/**
		 * @see JComponent#WHEN_IN_FOCUSED_WINDOW
		 */
		WHEN_IN_FOCUSED_WINDOW(JComponent.WHEN_IN_FOCUSED_WINDOW),
		/**
		 * @see JComponent#WHEN_FOCUSED
		 */
		WHEN_FOCUSED(JComponent.WHEN_FOCUSED),
		/**
		 * @see JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		 */
		WHEN_ANCESTOR_OF_FOCUSED_COMPONENT(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		private final int value;

		FocusCondition(int value) {
			this.value = value;
		}
	}
}

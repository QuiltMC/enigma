package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;

public final class KeyBindsConfig extends ReflectiveConfig.Section {

	private KeyBindsConfig() {
	}


	private static ConfigSection getSection(KeyBind keyBind) {
		return keyBind.category().isEmpty() ? cfg.data() : cfg.data().section(keyBind.category());
	}

	public static String[] getKeyBindCodes(KeyBind keyBind) {
		return getSection(keyBind).getArray(keyBind.name()).orElse(keyBind.serializeCombinations());
	}

	public static void setKeyBind(KeyBind keyBind) {
		getSection(keyBind).setArray(keyBind.name(), keyBind.serializeCombinations());
	}

	private static class KeyBindConfig extends ReflectiveConfig.Section {
		public final TrackedValue
	}
}

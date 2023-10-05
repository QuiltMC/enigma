package org.quiltmc.enigma.gui.config;

import org.quiltmc.enigma.config.ConfigContainer;
import org.quiltmc.enigma.config.ConfigSection;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;

public final class KeyBindsConfig {
	private KeyBindsConfig() {
	}

	private static final ConfigContainer cfg = ConfigContainer.getOrCreate("enigma/enigmakeybinds");

	public static void save() {
		cfg.save();
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
}

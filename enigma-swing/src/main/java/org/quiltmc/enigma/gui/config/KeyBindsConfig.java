package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;

public final class KeyBindsConfig extends ReflectiveConfig.Section {
	public final TrackedValue<ValueMap<String[]>> keyCodes = this.map(new String[]{""}).build();

	public String[] getKeyCodes(KeyBind keyBind) {
		String[] codes = this.keyCodes.value().get(keyBind.name());
		return codes.length == 0 ? keyBind.serializeCombinations() : codes;
	}

	public void setBind(KeyBind keyBind) {
		this.keyCodes.value().put(keyBind.name(), keyBind.serializeCombinations());
	}
}

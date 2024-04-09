package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueList;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public final class KeyBindConfig extends ReflectiveConfig {
	public final TrackedValue<ValueMap<ValueList<String>>> keyCodes = this.map(ValueList.create("")).build();

	public String[] getKeyCodes(KeyBind keyBind) {
		ValueList<String> codes = this.keyCodes.value().get(keyBind.name());
		return (codes == null || codes.isEmpty()) ? keyBind.serializeCombinations() : codes.toArray(String[]::new);
	}

	public void setBind(KeyBind keyBind) {
		this.keyCodes.value().put(keyBind.name(), ValueList.create("", keyBind.serializeCombinations()));
	}
}

package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.config.ConfigContainer;
import org.quiltmc.enigma.api.config.ConfigSection;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerPreferences;

import java.util.HashSet;
import java.util.Map;

public class DecompilerConfig extends ReflectiveConfig.Section {
	public final TrackedValue<Decompiler> decompiler = this.value(Decompiler.VINEFLOWER);

	private static final String VINEFLOWER = Decompiler.VINEFLOWER.name();

	private DecompilerConfig() {
	}

	private static final ConfigContainer cfg = ConfigContainer.getOrCreate("enigma/decompilers");

	public static void save() {
		cfg.save();
	}

	private static ConfigSection getVineflowerSection() {
		return cfg.data().section(VINEFLOWER);
	}

	public static void updateVineflowerValues(Map<String, Object> options) {
		ConfigSection section = getVineflowerSection();
		new HashSet<>(section.values().keySet()).forEach(section::remove);

		for (Map.Entry<String, Object> entry : options.entrySet()) {
			if (entry.getValue() instanceof String s) {
				section.setString(entry.getKey(), s);
			} else if (entry.getValue() instanceof Integer i) {
				section.setInt(entry.getKey(), i);
			} else if (entry.getValue() instanceof Boolean b) {
				section.setBool(entry.getKey(), b);
			}
		}
	}

	public static void bootstrap() {
		// Just run the static initialization
	}

	static {
		VineflowerPreferences.OPTIONS.putAll(getVineflowerSection().values());
	}
}

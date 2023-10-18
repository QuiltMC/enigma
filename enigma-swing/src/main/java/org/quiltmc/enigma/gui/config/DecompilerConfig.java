package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.api.config.ConfigContainer;
import org.quiltmc.enigma.api.config.ConfigSection;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerPreferences;

import javax.sound.midi.Track;
import java.util.HashSet;
import java.util.Map;

public class DecompilerConfig extends ReflectiveConfig.Section {
	public final TrackedValue<Decompiler> decompiler = this.value(Decompiler.VINEFLOWER);
	public final VineflowerSection vineflowerSection = new VineflowerSection();

	private static final String VINEFLOWER = Decompiler.VINEFLOWER.name();

	private DecompilerConfig() {
	}

	private class VineflowerSection extends ReflectiveConfig.Section {
		public final TrackedValue<ValueMap<String>> stringValues;
		public final TrackedValue<ValueMap<Integer>> intValues;
		public final TrackedValue<ValueMap<Boolean>>
	}

	public static void updateVineflowerValues(Map<String, Object> options) {
		this.vineflowerSection.

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

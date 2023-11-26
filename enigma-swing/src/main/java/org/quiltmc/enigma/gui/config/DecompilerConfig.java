package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerPreferences;

import java.util.Map;

public class DecompilerConfig extends ReflectiveConfig.Section {
	public final TrackedValue<Decompiler> decompiler = this.value(Decompiler.VINEFLOWER);
	public final TrackedValue<VineflowerSection> vineflowerSection = this.value(new VineflowerSection());

	private static class VineflowerSection extends ReflectiveConfig.Section {
		public final TrackedValue<ValueMap<String>> stringValues = this.map("").build();
		public final TrackedValue<ValueMap<Integer>> intValues = this.map(0).build();
		public final TrackedValue<ValueMap<Boolean>> booleanValues = this.map(true).build();
	}

	public static void updateVineflowerValues(Map<String, Object> options) {
		VineflowerSection section = getVineflowerSection();

		for (Map.Entry<String, Object> entry : options.entrySet()) {
			if (entry.getValue() instanceof String s) {
				section.stringValues.value().put(entry.getKey(), s);
			} else if (entry.getValue() instanceof Integer i) {
				section.intValues.value().put(entry.getKey(), i);
			} else if (entry.getValue() instanceof Boolean b) {
				section.booleanValues.value().put(entry.getKey(), b);
			}
		}
	}

	public static VineflowerSection getVineflowerSection() {
		return Config.decompiler().vineflowerSection.value();
	}

	public static void bootstrap() {
		// Just run the static initialization
	}

	static {
		VineflowerPreferences.OPTIONS.putAll(getVineflowerSection().stringValues.value());
		VineflowerPreferences.OPTIONS.putAll(getVineflowerSection().intValues.value());
		VineflowerPreferences.OPTIONS.putAll(getVineflowerSection().booleanValues.value());
	}
}

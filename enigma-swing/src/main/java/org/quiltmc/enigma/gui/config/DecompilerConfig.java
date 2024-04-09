package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.Processor;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.impl.source.vineflower.VineflowerPreferences;

import java.util.Map;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class DecompilerConfig extends ReflectiveConfig {
	public DecompilerConfig() {
		VineflowerPreferences.OPTIONS.putAll(this.vineflower.stringValues.value());
		VineflowerPreferences.OPTIONS.putAll(this.vineflower.intValues.value());
		VineflowerPreferences.OPTIONS.putAll(this.vineflower.booleanValues.value());
	}

	public final TrackedValue<Decompiler> activeDecompiler = this.value(Decompiler.VINEFLOWER);
	@Comment("The options passed to the Vineflower decompiler. What these do can be found here: https://vineflower.org/usage/.")
	public final VineflowerSection vineflower = new VineflowerSection();

	public static final class VineflowerSection extends Section {
		@Processor("processStrings")
		public final TrackedValue<ValueMap<String>> stringValues = this.map("").build();
		@Processor("processIntegers")
		public final TrackedValue<ValueMap<Integer>> intValues = this.map(0).build();
		@Processor("processBooleans")
		public final TrackedValue<ValueMap<Boolean>> booleanValues = this.map(true).build();

		@SuppressWarnings("unused")
		public void processStrings(TrackedValue.Builder<ValueMap<String>> builder) {
			builder.callback(map -> VineflowerPreferences.OPTIONS.putAll(map.value()));
		}

		@SuppressWarnings("unused")
		public void processIntegers(TrackedValue.Builder<ValueMap<Integer>> builder) {
			builder.callback(map -> VineflowerPreferences.OPTIONS.putAll(map.value()));
		}

		@SuppressWarnings("unused")
		public void processBooleans(TrackedValue.Builder<ValueMap<Boolean>> builder) {
			builder.callback(map -> VineflowerPreferences.OPTIONS.putAll(map.value()));
		}
	}

	public static void updateVineflowerValues(Map<String, Object> options) {
		Config.decompiler().vineflower.stringValues.value().clear();
		Config.decompiler().vineflower.intValues.value().clear();
		Config.decompiler().vineflower.booleanValues.value().clear();

		for (Map.Entry<String, Object> entry : options.entrySet()) {
			if (entry.getValue() instanceof String s) {
				Config.decompiler().vineflower.stringValues.value().put(entry.getKey(), s);
			} else if (entry.getValue() instanceof Integer i) {
				Config.decompiler().vineflower.intValues.value().put(entry.getKey(), i);
			} else if (entry.getValue() instanceof Boolean b) {
				Config.decompiler().vineflower.booleanValues.value().put(entry.getKey(), b);
			}
		}
	}
}

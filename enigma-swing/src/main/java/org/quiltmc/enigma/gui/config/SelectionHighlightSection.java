package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class SelectionHighlightSection extends ReflectiveConfig.Section {
	@Comment("The number of times the highlighting blinks. Set to 0 to disable highlighting.")
	public final TrackedValue<BoundedNumber<Integer>> blinks = this.value(new BoundedNumber<>(3, 0, 10));

	@Comment("The number of milliseconds the highlighting should be on and then off when blinking.")
	public final TrackedValue<BoundedNumber<Integer>> blinkDelay = this.value(new BoundedNumber<>(200, 10, 5000));

	public int getBlinks() {
		return this.blinks.value().value();
	}

	public int getBlinkDelay() {
		return this.blinkDelay.value().value();
	}
}

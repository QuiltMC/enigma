package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.IntegerRange;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class SelectionHighlightSection extends ReflectiveConfig.Section {
	@Comment("The number of times the highlighting blinks. Set to 0 to disable highlighting.")
	@IntegerRange(min = 0, max = 10)
	public final TrackedValue<Integer> blinks = this.value(3);

	@Comment("The number of milliseconds the highlighting should be on and then off when blinking.")
	@IntegerRange(min = 10, max = 5000)
	public final TrackedValue<Integer> blinkDelay = this.value(200);
}

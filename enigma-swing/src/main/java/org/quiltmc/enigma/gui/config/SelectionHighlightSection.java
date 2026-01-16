package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.IntegerRange;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class SelectionHighlightSection extends ReflectiveConfig.Section {
	public static final int MIN_BLINKS = 0;
	public static final int MAX_BLINKS = 10;
	public static final int MIN_BLINK_DELAY = 10;
	public static final int MAX_BLINK_DELAY = 5000;

	@Comment("The number of times the highlighting blinks. Set to 0 to disable highlighting.")
	@IntegerRange(min = MIN_BLINKS, max = MAX_BLINKS)
	public final TrackedValue<Integer> blinks = this.value(3);

	@Comment("The milliseconds the highlighting should be on and then off when blinking.")
	@IntegerRange(min = MIN_BLINK_DELAY, max = MAX_BLINK_DELAY)
	public final TrackedValue<Integer> blinkDelay = this.value(200);
}

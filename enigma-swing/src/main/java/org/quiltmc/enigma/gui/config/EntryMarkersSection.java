package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.IntegerRange;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class EntryMarkersSection extends ReflectiveConfig.Section {
	public static final int MIN_MAX_MARKERS_PER_LINE = 0;
	public static final int MAX_MAX_MARKERS_PER_LINE = 3;

	@Comment("Whether markers should have tooltips showing their corresponding entries.")
	public final TrackedValue<Boolean> tooltip = this.value(true);

	@Comment("The maximum number of markers to show for a single line. Set to 0 to disable markers.")
	@IntegerRange(min = MIN_MAX_MARKERS_PER_LINE, max = MAX_MAX_MARKERS_PER_LINE)
	public final TrackedValue<Integer> maxMarkersPerLine = this.value(2);

	@Comment("Whether only declaration entries should be marked.")
	public final TrackedValue<Boolean> onlyMarkDeclarations = this.value(true);

	@Comment("Whether obfuscated entries should be marked.")
	public final TrackedValue<Boolean> markObfuscated = this.value(true);

	@Comment("Whether fallback entries should be marked.")
	public final TrackedValue<Boolean> markFallback = this.value(true);

	@Comment("Whether proposed entries should be marked.")
	public final TrackedValue<Boolean> markProposed = this.value(false);

	@Comment("Whether deobfuscated entries should be marked.")
	public final TrackedValue<Boolean> markDeobfuscated = this.value(false);
}

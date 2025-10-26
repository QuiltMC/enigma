package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class EntryMarkersSection extends ReflectiveConfig.Section {
	@Comment("Whether markers can be clicked to navigate to their corresponding entries.")
	public final TrackedValue<Boolean> interactable = this.value(true);

	@Comment("Whether obfuscated entries should be marked.")
	public final TrackedValue<Boolean> markObfuscated = this.value(true);

	@Comment("Whether fallback entries should be marked.")
	public final TrackedValue<Boolean> markFallback = this.value(true);

	@Comment("Whether proposed entries should be marked.")
	public final TrackedValue<Boolean> markProposed = this.value(false);

	@Comment("Whether deobfuscated entries should be marked.")
	public final TrackedValue<Boolean> markDeobfuscated = this.value(false);
}

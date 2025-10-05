package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class EditorTooltipSection extends ReflectiveConfig.Section {
	@Comment("Whether tooltips are enabled.")
	public final TrackedValue<Boolean> enable = this.value(true);

	@Comment("Whether tooltips can be clicked and interacted with to navigate their content.")
	public final TrackedValue<Boolean> interactable = this.value(true);
}

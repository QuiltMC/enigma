package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class EditorConfig extends ReflectiveConfig {
	@Comment("Whether editors' quick find toolbars should remain visible when they lose focus.")
	public final TrackedValue<Boolean> persistentQuickFind = this.value(true);

	@Comment("The settings for the editor's entry tooltip.")
	public final EditorTooltipSection entryTooltip = new EditorTooltipSection();
}

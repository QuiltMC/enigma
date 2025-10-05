package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class EditorConfig extends ReflectiveConfig {
	@Comment("The settings for the editor tooltip.")
	public final EditorTooltipSection tooltip = new EditorTooltipSection();
}

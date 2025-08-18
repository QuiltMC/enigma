package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class FeaturesSection extends ReflectiveConfig.Section {
	@Comment("Enables statistic icons in the class tree. This has a major performance impact on JAR files with lots of classes.")
	public final TrackedValue<Boolean> enableClassTreeStatIcons = this.value(true);
	@Comment("Enables auto save functionality, which will automatically save mappings when a change is made.")
	public final TrackedValue<Boolean> autoSaveMappings = this.value(false);
}

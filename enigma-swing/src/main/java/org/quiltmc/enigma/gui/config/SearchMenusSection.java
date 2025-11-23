package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class SearchMenusSection extends ReflectiveConfig.Section {
	@Comment("Whether to show the search menus preview hint until it's dismissed.")
	public final TrackedValue<Boolean> showPreviewHint = this.value(true);

	@Comment("Whether to show the search menus execute hint until it's dismissed.")
	public final TrackedValue<Boolean> showExecuteHint = this.value(true);
}

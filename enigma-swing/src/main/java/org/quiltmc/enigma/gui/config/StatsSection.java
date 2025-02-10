package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class StatsSection extends ReflectiveConfig.Section {
	public final TrackedValue<String> lastSelectedDir = this.value("");
	public final TrackedValue<String> lastTopLevelPackage = this.value("");
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
	public final TrackedValue<Boolean> shouldCountFallbackNames = this.value(false);
	public final IconsSection icons = new IconsSection();

	@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
	public static class IconsSection extends ReflectiveConfig.Section {
		public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
		public final TrackedValue<Boolean> shouldCountFallbackNames = this.value(false);
	}
}

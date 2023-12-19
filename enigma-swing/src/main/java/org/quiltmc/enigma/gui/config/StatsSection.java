package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedName;
import org.quiltmc.config.api.values.TrackedValue;

public class StatsSection extends ReflectiveConfig.Section {
	@SerializedName("last_selected_dir")
	public final TrackedValue<String> lastSelectedDir = this.value("");
	@SerializedName("last_top_level_package")
	public final TrackedValue<String> lastTopLevelPackage = this.value("");
	@SerializedName("should_include_synthetic_parameters")
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
}

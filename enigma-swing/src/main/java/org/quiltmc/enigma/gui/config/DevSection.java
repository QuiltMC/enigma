package org.quiltmc.enigma.gui.config;

import com.google.gson.annotations.SerializedName;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;

public class DevSection extends ReflectiveConfig.Section {
	@SerializedName("show_mapping_source_plugin")
	public final TrackedValue<Boolean> showMappingSourcePlugin = this.value(false);
}

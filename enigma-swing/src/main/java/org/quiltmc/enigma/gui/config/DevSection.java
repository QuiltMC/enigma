package org.quiltmc.enigma.gui.config;

import com.google.gson.annotations.SerializedName;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Processor;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.source.DecompiledClassSource;

public class DevSection extends ReflectiveConfig.Section {
	@SerializedName("show_mapping_source_plugin")
	public final TrackedValue<Boolean> showMappingSourcePlugin = this.value(false);

	@SerializedName("debug_token_highlights")
	@Processor("processDebugTokenHighlights")
	public final TrackedValue<Boolean> debugTokenHighlights = this.value(false);

	@SuppressWarnings("unused")
	public void processDebugTokenHighlights(TrackedValue.Builder<Boolean> builder) {
		builder.callback(trackedValue -> DecompiledClassSource.DEBUG_TOKEN_HIGHLIGHTS = trackedValue.value());
	}
}

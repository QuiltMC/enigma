package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Processor;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.gui.network.IntegratedEnigmaClient;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class DevSection extends ReflectiveConfig.Section {
	public transient boolean anyEnabled = false;

	public final TrackedValue<Boolean> showMappingSourcePlugin = this.value(false);

	@Processor("processDebugTokenHighlights")
	public final TrackedValue<Boolean> debugTokenHighlights = this.value(false);

	@Processor("processLogClientPackets")
	public final TrackedValue<Boolean> logClientPackets = this.value(false);

	@SuppressWarnings("unused")
	public void processDebugTokenHighlights(TrackedValue.Builder<Boolean> builder) {
		builder.callback(trackedValue -> DecompiledClassSource.DEBUG_TOKEN_HIGHLIGHTS = trackedValue.value());
	}

	@SuppressWarnings("unused")
	public void processLogClientPackets(TrackedValue.Builder<Boolean> builder) {
		builder.callback(trackedValue -> IntegratedEnigmaClient.LOG_PACKETS = trackedValue.value());
	}
}

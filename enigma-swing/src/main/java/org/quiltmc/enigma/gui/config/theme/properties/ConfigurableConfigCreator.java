package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;

public interface ConfigurableConfigCreator extends Config.Creator {
	void configure();
}

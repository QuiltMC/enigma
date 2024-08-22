package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLightLaf;

public class DefaultThemeProperties extends ConfigurableLafThemeProperties {
	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.DEFAULT;
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatLightLaf::new;
	}
}

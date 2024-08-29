package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;

public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	public ConfigurableFlatLightLaf(LookAndFeelProperties.Colors colors) {
		super(NAME, colors, Base.LIGHT);
	}
}

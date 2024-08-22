package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.ThemeCreator;

public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	public ConfigurableFlatLightLaf(ThemeCreator.LookAndFeelColors colors) {
		super(NAME, colors, Base.LIGHT);
	}
}

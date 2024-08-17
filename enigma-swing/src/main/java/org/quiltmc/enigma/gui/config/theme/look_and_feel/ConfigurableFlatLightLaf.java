package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.Theme;

// TODO: see if default colors can be loaded from FlatLightLaf.properties
public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	public static Theme.LookAndFeelColors.Builder createColors() {
		// default colors are for FlatLightLaf
		return new Theme.LookAndFeelColors.Builder();
	}

	public ConfigurableFlatLightLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors, Base.LIGHT);
	}
}

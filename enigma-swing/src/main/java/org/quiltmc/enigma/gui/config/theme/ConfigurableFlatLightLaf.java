package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatLightLaf;

// TODO: see if default colors can be loaded from FlatLightLaf.properties
public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	protected ConfigurableFlatLightLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors);
	}

	@Override
	protected String getBase() {
		return "light";
	}
	//
	// @Override
	// protected Theme.LookAndFeelColors.Builder buildColors(Theme.LookAndFeelColors.Builder colors) {
	// 	// defaults are already for flat light
	// 	return colors;
	// }
}

package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatLightLaf;

// TODO: see if default colors can be loaded from FlatLightLaf.properties
public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	public static Theme.LookAndFeelColors.Builder createColors() {
		// default colors are for FlatLightLaf
		return new Theme.LookAndFeelColors.Builder();
	}

	protected ConfigurableFlatLightLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors);
	}

	@Override
	protected String getBase() {
		return "light";
	}
}

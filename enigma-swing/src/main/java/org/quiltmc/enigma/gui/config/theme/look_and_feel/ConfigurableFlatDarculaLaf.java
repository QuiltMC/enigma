package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.quiltmc.enigma.gui.config.theme.Theme;

public class ConfigurableFlatDarculaLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatDarculaLaf.NAME;

	public static Theme.LookAndFeelColors.Builder createColors() {
		// FlatDarculaLaf doesn't override any of FlatDarkLaf's colors in FlatDarculaLaf.properties
		return ConfigurableFlatDarkLaf.createColors();
	}

	public ConfigurableFlatDarculaLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors, Base.DARCULA);
	}
}

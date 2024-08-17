package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatDarculaLaf;

public class ConfigurableFlatDarculaLaf extends AbstractConfigurableFlatDarkLaf {
	public static final String NAME = "Configurable " + FlatDarculaLaf.NAME;

	public static Theme.LookAndFeelColors.Builder createColors() {
		// FlatDarculaLaf doesn't override any of FlatDarkLaf's colors in FlatDarculaLaf.properties
		return ConfigurableFlatDarkLaf.createColors();
	}

	protected ConfigurableFlatDarculaLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors);
	}

	@Override
	protected Base getBase() {
		return Base.DARCULA;
	}
}

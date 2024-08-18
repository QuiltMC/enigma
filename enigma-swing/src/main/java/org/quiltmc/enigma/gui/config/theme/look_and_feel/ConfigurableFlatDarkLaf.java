package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatDarkLaf;
import org.quiltmc.enigma.gui.config.theme.Theme;

public class ConfigurableFlatDarkLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatDarkLaf.NAME;

	public ConfigurableFlatDarkLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors, Base.DARK);
	}
}

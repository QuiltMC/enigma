package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.Theme;

public class ConfigurableFlatLightLaf extends ConfigurableFlatLaf {
	public static final String NAME = "Configurable " + FlatLightLaf.NAME;

	public ConfigurableFlatLightLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors, Base.LIGHT);
	}
}

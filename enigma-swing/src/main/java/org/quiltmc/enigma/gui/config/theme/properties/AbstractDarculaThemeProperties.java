package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatDarkLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import java.util.List;

public abstract class AbstractDarculaThemeProperties extends ConfigurableFlatLafThemeProperties {
	protected AbstractDarculaThemeProperties(
			SyntaxPaneProperties syntaxPaneColors, LookAndFeelProperties lookAndFeelColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatDarkLaf::new;
	}
}

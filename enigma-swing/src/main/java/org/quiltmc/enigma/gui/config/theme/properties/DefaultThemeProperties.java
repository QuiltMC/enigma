package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import java.util.ArrayList;
import java.util.List;

public class DefaultThemeProperties extends ConfigurableFlatLafThemeProperties {
	public DefaultThemeProperties() {
		this(new SyntaxPaneProperties(), new LookAndFeelProperties(), new ArrayList<>());
	}

	protected DefaultThemeProperties(
			SyntaxPaneProperties syntaxPaneColors, LookAndFeelProperties lookAndFeelColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatLightLaf::new;
	}
}

package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLightLaf;

import java.util.ArrayList;
import java.util.List;

public class DefaultThemeProperties extends ConfigurableLafThemeProperties {
	public DefaultThemeProperties() {
		this(new SyntaxPaneColorProperties(), new LookAndFeelColorProperties(), new ArrayList<>());
	}

	protected DefaultThemeProperties(SyntaxPaneColorProperties syntaxPaneColors, LookAndFeelColorProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.DEFAULT;
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatLightLaf::new;
	}
}

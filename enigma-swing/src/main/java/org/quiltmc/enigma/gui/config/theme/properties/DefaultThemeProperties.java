package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLightLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import java.util.ArrayList;
import java.util.List;

public class DefaultThemeProperties extends ConfigurableLafThemeProperties {
	public DefaultThemeProperties() {
		this(new SyntaxPaneProperties(), new LookAndFeelProperties(), new ArrayList<>());
	}

	protected DefaultThemeProperties(SyntaxPaneProperties syntaxPaneColors, LookAndFeelProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
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

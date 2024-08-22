package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatDarkLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import java.util.List;

public abstract class AbstractDarculaThemeProperties extends ConfigurableLafThemeProperties {
	protected AbstractDarculaThemeProperties(SyntaxPaneProperties syntaxPaneColors, LookAndFeelProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatDarkLaf::new;
	}
}

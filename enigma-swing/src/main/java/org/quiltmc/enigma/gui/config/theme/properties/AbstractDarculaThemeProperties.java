package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatDarkLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;

import java.util.List;

public abstract class AbstractDarculaThemeProperties extends ConfigurableLafThemeProperties {
	protected AbstractDarculaThemeProperties(SyntaxPaneColorProperties syntaxPaneColors, LookAndFeelColorProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	protected ConfigurableFlatLaf.Constructor getLafConstructor() {
		return ConfigurableFlatDarkLaf::new;
	}
}

package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import java.util.List;

public abstract class ConfigurableFlatLafThemeProperties extends ConfigurableLafThemeProperties {
	protected ConfigurableFlatLafThemeProperties(SyntaxPaneProperties syntaxPaneColors, LookAndFeelProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	public final boolean needsScaling() {
		return false;
	}
}

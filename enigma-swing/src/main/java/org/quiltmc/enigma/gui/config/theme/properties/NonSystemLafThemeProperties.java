package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.*;
import java.util.List;

public abstract class NonSystemLafThemeProperties extends ThemeProperties {
	protected NonSystemLafThemeProperties(SyntaxPaneProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public final void setGlobalLaf() throws UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(this.getLaf());
	}

	protected abstract LookAndFeel getLaf();
}

package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SystemThemeProperties extends ThemeProperties {
	public SystemThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected SystemThemeProperties(SyntaxPaneProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.SYSTEM;
	}

	@Override
	public void setGlobalLaf() throws
			UnsupportedLookAndFeelException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	@Override
	public boolean needsScaling() {
		return true;
	}
}

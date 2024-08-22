package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SystemThemeProperties extends ThemeProperties {
	public SystemThemeProperties() {
		this(new SyntaxPaneColorProperties(), new ArrayList<>());
	}

	protected SystemThemeProperties(SyntaxPaneColorProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
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
}

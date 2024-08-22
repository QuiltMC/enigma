package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;

public class SystemThemeProperties extends ThemeProperties {
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

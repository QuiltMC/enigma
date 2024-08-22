package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;

public class NoneThemeProperties extends NonSystemLafThemeProperties {
	private static final LookAndFeel NONE_LAF = UIManager.getLookAndFeel();

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.NONE;
	}

	@Override
	protected LookAndFeel getLaf() {
		return NONE_LAF;
	}
}

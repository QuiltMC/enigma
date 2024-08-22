package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class MetalThemeProperties extends NonSystemLafThemeProperties {
	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.METAL;
	}

	@Override
	protected LookAndFeel getLaf() {
		return new MetalLookAndFeel();
	}
}

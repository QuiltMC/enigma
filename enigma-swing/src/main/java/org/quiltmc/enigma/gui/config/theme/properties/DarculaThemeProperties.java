package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

public class DarculaThemeProperties extends AbstractDarculaThemeProperties {
	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.DARCULA;
	}

	@Override
	protected LookAndFeelColors.Builder buildLookAndFeelColors(LookAndFeelColors.Builder lookAndFeelColors) {
		// colors are from FlatDarkLaf.properties
		return lookAndFeelColors
			.foreground(new SerializableColor(0xFFBBBBBB))
			.background(new SerializableColor(0xFF3C3F41))

			.accentBaseColor(new SerializableColor(0xFF4B6EAF))

			.activeCaption(new SerializableColor(0xFF434E60))
			.inactiveCaption(new SerializableColor(0xFF393C3D))

			.errorBorder(new SerializableColor(0xFF8B3C3C))
			.warningBorder(new SerializableColor(0xFFAC7920));
	}
}

package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.enigma.gui.config.theme.properties.ConfigurableLafThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

/**
 * Factory  methods for creating look and feel colors for themes.
 *
 * <p>
 * These can't be created in {@link ThemeProperties} subclasses because of a quilt-config limitation.
 */
public class LookAndFeelColorsFactories {
	public static ConfigurableLafThemeProperties.LookAndFeelColors.Builder createLight() {
		// default colors are light
		return new ConfigurableLafThemeProperties.LookAndFeelColors.Builder();
	}

	public static ConfigurableLafThemeProperties.LookAndFeelColors.Builder createDarcula() {
		// colors are from FlatDarkLaf.properties
		return new ConfigurableLafThemeProperties.LookAndFeelColors.Builder()
			.foreground(new ThemeProperties.SerializableColor(0xFFBBBBBB))
			.background(new ThemeProperties.SerializableColor(0xFF3C3F41))

			.accentBaseColor(new ThemeProperties.SerializableColor(0xFF4B6EAF))

			.activeCaption(new ThemeProperties.SerializableColor(0xFF434E60))
			.inactiveCaption(new ThemeProperties.SerializableColor(0xFF393C3D))

			.errorBorder(new ThemeProperties.SerializableColor(0xFF8B3C3C))
			.warningBorder(new ThemeProperties.SerializableColor(0xFFAC7920));
	}

	public static ConfigurableLafThemeProperties.LookAndFeelColors.Builder createDarcerula() {
		return createDarcula()
			.foreground(new ThemeProperties.SerializableColor(0xFFC3C3C3))
			.background(new ThemeProperties.SerializableColor(0xFF242729))

			.accentBaseColor(new ThemeProperties.SerializableColor(0xFF4366A7))

			.activeCaption(new ThemeProperties.SerializableColor(0xFF374254))
			.inactiveCaption(new ThemeProperties.SerializableColor(0xFF2B2E2F));

			// errorBorder inherited from darcula
			// warningBorder inherited from darcula
	}
}

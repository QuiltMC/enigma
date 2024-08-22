package org.quiltmc.enigma.gui.config.theme;

/**
 * Factory  methods for creating look and feel colors for themes.
 *
 * <p>
 * These can't be created in {@link ThemeCreator} subclasses because of a quilt-config limitation.
 */
public class LookAndFeelColorsFactories {
	public static ThemeCreator.LookAndFeelColors.Builder createLight() {
		// default colors are light
		return new ThemeCreator.LookAndFeelColors.Builder();
	}

	public static ThemeCreator.LookAndFeelColors.Builder createDarcula() {
		// colors are from FlatDarkLaf.properties
		return new ThemeCreator.LookAndFeelColors.Builder()
			.foreground(new ThemeCreator.SerializableColor(0xFFBBBBBB))
			.background(new ThemeCreator.SerializableColor(0xFF3C3F41))

			.accentBaseColor(new ThemeCreator.SerializableColor(0xFF4B6EAF))

			.activeCaption(new ThemeCreator.SerializableColor(0xFF434E60))
			.inactiveCaption(new ThemeCreator.SerializableColor(0xFF393C3D))

			.errorBorder(new ThemeCreator.SerializableColor(0xFF8B3C3C))
			.warningBorder(new ThemeCreator.SerializableColor(0xFFAC7920));
	}

	public static ThemeCreator.LookAndFeelColors.Builder createDarcerula() {
		return createDarcula()
			.foreground(new ThemeCreator.SerializableColor(0xFFC3C3C3))
			.background(new ThemeCreator.SerializableColor(0xFF242729))

			.accentBaseColor(new ThemeCreator.SerializableColor(0xFF4366A7))

			.activeCaption(new ThemeCreator.SerializableColor(0xFF374254))
			.inactiveCaption(new ThemeCreator.SerializableColor(0xFF2B2E2F));

			// errorBorder inherited from darcula
			// warningBorder inherited from darcula
	}
}

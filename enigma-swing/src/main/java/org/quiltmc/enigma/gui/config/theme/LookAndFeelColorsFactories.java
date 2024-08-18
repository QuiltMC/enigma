package org.quiltmc.enigma.gui.config.theme;

/**
 * Factory  methods for creating look and feel colors for themes.
 *
 * <p>
 * These can't be created in {@link Theme} subclasses because of a quilt-config limitation.
 */
public class LookAndFeelColorsFactories {
	public static Theme.LookAndFeelColors.Builder createLight() {
		// default colors are light
		return new Theme.LookAndFeelColors.Builder();
	}

	public static Theme.LookAndFeelColors.Builder createDarcula() {
		// colors are from FlatDarkLaf.properties
		return new Theme.LookAndFeelColors.Builder()
			.foreground(new Theme.SerializableColor(0xFFBBBBBB))
			.background(new Theme.SerializableColor(0xFF3C3F41))

			.accentBaseColor(new Theme.SerializableColor(0xFF4B6EAF))

			.activeCaption(new Theme.SerializableColor(0xFF434E60))
			.inactiveCaption(new Theme.SerializableColor(0xFF393C3D))

			.errorBorder(new Theme.SerializableColor(0xFF8B3C3C))
			.warningBorder(new Theme.SerializableColor(0xFFAC7920));
	}

	public static Theme.LookAndFeelColors.Builder createDarcerula() {
		return createDarcula()
			.foreground(new Theme.SerializableColor(0xFFC3C3C3))
			.background(new Theme.SerializableColor(0xFF242729))

			.accentBaseColor(new Theme.SerializableColor(0xFF4366A7))

			.activeCaption(new Theme.SerializableColor(0xFF374254))
			.inactiveCaption(new Theme.SerializableColor(0xFF2B2E2F));

			// errorBorder inherited from darcula
			// warningBorder inherited from darcula
	}
}

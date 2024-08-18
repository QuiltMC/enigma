package org.quiltmc.enigma.gui.config.theme;

// TODO: see if default colors can be loaded from FlatLightLaf/FlatDarkLaf.properties
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
		// TODO
		return createDarcula();
	}
}

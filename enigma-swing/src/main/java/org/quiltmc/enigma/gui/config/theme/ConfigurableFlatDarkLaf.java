package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatDarkLaf;

// TODO: see if default colors can be loaded from FlatDarkLaf.properties
// TODO: investigate 'change theme restart' alert button color
public class ConfigurableFlatDarkLaf extends AbstractConfigurableFlatDarkLaf {
	public static final String NAME = "Configurable " + FlatDarkLaf.NAME;

	public static Theme.LookAndFeelColors.Builder createColors() {
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

	protected ConfigurableFlatDarkLaf(Theme.LookAndFeelColors colors) {
		super(NAME, colors);
	}

	@Override
	protected Base getBase() {
		return Base.DARK;
	}
}

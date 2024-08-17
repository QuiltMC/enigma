package org.quiltmc.enigma.gui.config.theme;

public abstract class AbstractConfigurableFlatDarkLaf extends ConfigurableFlatLaf {
	protected AbstractConfigurableFlatDarkLaf(String name, Theme.LookAndFeelColors colors) {
		super(name, colors);

		// FlatLightLaf.properties has:
		// 		Button.focusedBackground = changeLightness($Component.focusColor,95%)
		// FlatDarkLaf.properties lacks any equivalent,
		// 		so some buttons have a light background with very low contrast against the foreground.
		// This adds a rough equivalent to FlatLightLaf's property.
		this.getProperties().setProperty(
				"Button.focusedBackground",
				"lighten($Component.focusColor,5%,derived noAutoInverse)"
		);
	}
}

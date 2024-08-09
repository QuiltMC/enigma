package org.quiltmc.enigma.gui.config.theme;

public class DefaultTheme extends Theme {
	public DefaultTheme(LookAndFeel lookAndFeel) {
		super(lookAndFeel);
	}

	@Override
	protected Colors.Builder buildDefaultColors(Colors.Builder colors) {
		return colors;
	}
}

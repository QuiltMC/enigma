package org.quiltmc.enigma.gui.config.theme.properties;

import javax.swing.*;

public abstract class NonSystemLafThemeProperties extends ThemeProperties {
	@Override
	public final void setGlobalLaf() throws UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(this.getLaf());
	}

	protected abstract LookAndFeel getLaf();
}

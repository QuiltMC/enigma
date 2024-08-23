package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.List;

public abstract class NonSystemLafThemeProperties extends ThemeProperties {
	protected NonSystemLafThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public final void setGlobalLaf() throws UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(this.getLaf());
	}

	protected abstract LookAndFeel getLaf();
}

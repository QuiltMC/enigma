package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.util.ArrayList;
import java.util.List;

public class NoneThemeProperties extends NonConfigurableLafThemeProperties {
	private static final LookAndFeel NONE_LAF = UIManager.getLookAndFeel();

	public NoneThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected NoneThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public boolean onlyScaleFonts() {
		return false;
	}

	@Override
	protected LookAndFeel getLaf() {
		return NONE_LAF;
	}
}

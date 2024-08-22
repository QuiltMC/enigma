package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class NoneThemeProperties extends NonSystemLafThemeProperties {
	private static final LookAndFeel NONE_LAF = UIManager.getLookAndFeel();

	public NoneThemeProperties() {
		this(new SyntaxPaneColorProperties(), new ArrayList<>());
	}

	protected NoneThemeProperties(SyntaxPaneColorProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.NONE;
	}

	@Override
	protected LookAndFeel getLaf() {
		return NONE_LAF;
	}
}

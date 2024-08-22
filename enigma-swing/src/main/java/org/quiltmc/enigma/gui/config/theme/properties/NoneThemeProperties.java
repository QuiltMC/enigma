package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class NoneThemeProperties extends NonSystemLafThemeProperties {
	private static final LookAndFeel NONE_LAF = UIManager.getLookAndFeel();

	public NoneThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected NoneThemeProperties(SyntaxPaneProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public boolean needsScaling() {
		return true;
	}

	@Override
	protected LookAndFeel getLaf() {
		return NONE_LAF;
	}
}

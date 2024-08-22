package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.ArrayList;
import java.util.List;

public class MetalThemeProperties extends NonSystemLafThemeProperties {
	public MetalThemeProperties() {
		this(new SyntaxPaneColorProperties(), new ArrayList<>());
	}

	protected MetalThemeProperties(SyntaxPaneColorProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.METAL;
	}

	@Override
	protected LookAndFeel getLaf() {
		return new MetalLookAndFeel();
	}
}

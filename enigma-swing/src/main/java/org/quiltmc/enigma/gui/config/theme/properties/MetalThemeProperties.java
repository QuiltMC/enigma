package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.ArrayList;
import java.util.List;

public class MetalThemeProperties extends NonSystemLafThemeProperties {
	public MetalThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected MetalThemeProperties(SyntaxPaneProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.METAL;
	}

	@Override
	public boolean needsScaling() {
		return true;
	}

	@Override
	protected LookAndFeel getLaf() {
		return new MetalLookAndFeel();
	}
}

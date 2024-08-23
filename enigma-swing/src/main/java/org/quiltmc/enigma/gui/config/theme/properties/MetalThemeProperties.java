package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;

import javax.swing.LookAndFeel;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.ArrayList;
import java.util.List;

public class MetalThemeProperties extends NonConfigurableLafThemeProperties {
	public MetalThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected MetalThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, creators);
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

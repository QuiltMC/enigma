package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.util.ListUtil;

import javax.swing.LookAndFeel;
import java.util.List;

public abstract class ConfigurableLafThemeProperties extends NonSystemLafThemeProperties {
	private final LookAndFeelProperties lookAndFeelProperties;

	protected ConfigurableLafThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			LookAndFeelProperties lookAndFeelColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, ListUtil.prepend(lookAndFeelColors, creators));
		this.lookAndFeelProperties = lookAndFeelColors;
	}

	@Override
	protected final LookAndFeel getLaf() {
		return this.getLafConstructor().construct(this.lookAndFeelProperties.colors);
	}

	protected abstract ConfigurableFlatLaf.Constructor getLafConstructor();
}

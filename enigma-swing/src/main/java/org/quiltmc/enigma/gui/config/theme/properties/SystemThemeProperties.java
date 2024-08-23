package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.util.ListUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SystemThemeProperties extends ThemeProperties {
	public SystemThemeProperties() {
		this(new SyntaxPaneProperties(), new ArrayList<>());
	}

	protected SystemThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(
			syntaxPaneColors,
			// this is duplicated from NonConfigurableLafThemeProperties
			// because java doesn't support multi-inheritance
			ListUtil.prepend(NonConfigurableLafThemeProperties::createComment, creators)
		);
	}

	@Override
	public void setGlobalLaf() throws
			UnsupportedLookAndFeelException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	@Override
	public boolean needsScaling() {
		return true;
	}
}

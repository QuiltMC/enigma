package org.quiltmc.enigma.gui.event;

import org.quiltmc.enigma.gui.config.theme.LookAndFeel;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.api.source.TokenType;

import java.util.Map;

public interface ThemeChangeListener {
	void onThemeChanged(LookAndFeel lookAndFeel, Map<TokenType, BoxHighlightPainter> boxHighlightPainters);
}

package org.quiltmc.enigma.gui.events;

import org.quiltmc.enigma.gui.config.LookAndFeel;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.source.RenamableTokenType;

import java.util.Map;

public interface ThemeChangeListener {
	void onThemeChanged(LookAndFeel lookAndFeel, Map<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters);
}

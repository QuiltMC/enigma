package cuchaz.enigma.gui.events;

import cuchaz.enigma.gui.config.LookAndFeel;
import cuchaz.enigma.gui.highlight.BoxHighlightPainter;
import cuchaz.enigma.source.RenamableTokenType;

import java.util.Map;

public interface ThemeChangeListener {
	void onThemeChanged(LookAndFeel lookAndFeel, Map<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters);
}

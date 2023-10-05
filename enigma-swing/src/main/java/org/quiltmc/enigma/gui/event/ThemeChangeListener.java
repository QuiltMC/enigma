package org.quiltmc.enigma.gui.event;

import org.quiltmc.enigma.gui.config.LookAndFeel;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.api.source.RenamableTokenType;

import java.util.Map;

public interface ThemeChangeListener {
	void onThemeChanged(LookAndFeel lookAndFeel, Map<RenamableTokenType, BoxHighlightPainter> boxHighlightPainters);
}

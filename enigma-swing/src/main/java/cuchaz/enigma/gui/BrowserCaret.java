package cuchaz.enigma.gui;

import javax.swing.text.DefaultCaret;

public class BrowserCaret extends DefaultCaret {
	@Override
	public boolean isSelectionVisible() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}
}

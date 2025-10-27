package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.SelectionHighlightSection;
import org.quiltmc.enigma.gui.element.IntRangeConfigMenuItem;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

public class SelectionHighlightMenu extends AbstractEnigmaMenu {
	private final IntRangeConfigMenuItem blinks;
	private final IntRangeConfigMenuItem blinkDelay;

	protected SelectionHighlightMenu(Gui gui) {
		super(gui);

		final SelectionHighlightSection config = Config.editor().selectionHighlight;

		this.blinks = new IntRangeConfigMenuItem(
				gui, config.blinks,
				SelectionHighlightSection.MIN_BLINKS, SelectionHighlightSection.MAX_BLINKS,
				"menu.view.selection_highlight.blinks"
		);

		this.blinkDelay = new IntRangeConfigMenuItem(
				gui, config.blinkDelay,
				SelectionHighlightSection.MIN_BLINK_DELAY, SelectionHighlightSection.MAX_BLINK_DELAY,
				"menu.view.selection_highlight.blink_delay"
		);

		this.add(this.blinks);
		this.add(this.blinkDelay);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.selection_highlight"));

		this.blinks.retranslate();
		this.blinkDelay.retranslate();
	}
}

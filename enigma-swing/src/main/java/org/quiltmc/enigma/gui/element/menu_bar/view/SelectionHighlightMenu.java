package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.SelectionHighlightSection;
import org.quiltmc.enigma.gui.element.IntRangeConfigMenuItem;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleEnigmaMenu;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

public class SelectionHighlightMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.selection_highlight";

	private final SimpleEnigmaMenu blinksMenu;
	private final IntRangeConfigMenuItem blinkDelay;

	protected SelectionHighlightMenu(Gui gui) {
		super(gui);

		final SelectionHighlightSection config = Config.editor().selectionHighlight;

		this.blinksMenu = GuiUtil.createIntConfigRadioMenu(
				gui, "menu.view.selection_highlight.blinks",
				config.blinks, SelectionHighlightSection.MIN_BLINKS, SelectionHighlightSection.MAX_BLINKS
		);

		this.blinkDelay = new IntRangeConfigMenuItem(
				gui, config.blinkDelay,
				SelectionHighlightSection.MIN_BLINK_DELAY, SelectionHighlightSection.MAX_BLINK_DELAY,
				"menu.view.selection_highlight.blink_delay"
		);

		this.add(this.blinksMenu);
		this.add(this.blinkDelay);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));

		this.blinksMenu.retranslate();
		this.blinkDelay.retranslate();
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}

	private static class BlinksMenu extends AbstractSearchableEnigmaMenu {
		protected BlinksMenu(Gui gui) {
			super(gui);
		}

		@Override
		public String getAliasesTranslationKeyPrefix() {
			return "menu.view.selection_highlight.blinks";
		}
	}
}

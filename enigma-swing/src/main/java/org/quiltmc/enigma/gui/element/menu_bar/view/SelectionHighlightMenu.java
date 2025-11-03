package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.SelectionHighlightSection;
import org.quiltmc.enigma.gui.element.IntRangeConfigMenuItem;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenu;

public class SelectionHighlightMenu extends AbstractEnigmaMenu {
	private final JMenu blinksMenu;
	private final IntRangeConfigMenuItem blinkDelay;

	protected SelectionHighlightMenu(Gui gui) {
		super(gui);

		final SelectionHighlightSection config = Config.editor().selectionHighlight;

		this.blinksMenu = GuiUtil.createIntConfigRadioMenu(
				config.blinks,
				SelectionHighlightSection.MIN_BLINKS, SelectionHighlightSection.MAX_BLINKS,
				this::retranslateBlinksMenu
		);

		this.blinkDelay = new IntRangeConfigMenuItem(
				gui, config.blinkDelay,
				SelectionHighlightSection.MIN_BLINK_DELAY, SelectionHighlightSection.MAX_BLINK_DELAY, 100,
				"menu.view.selection_highlight.blink_delay"
		);

		this.add(this.blinksMenu);
		this.add(this.blinkDelay);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.selection_highlight"));

		this.retranslateBlinksMenu();
		this.blinkDelay.retranslate();
	}

	private void retranslateBlinksMenu() {
		this.blinksMenu.setText(I18n.translateFormatted(
				"menu.view.selection_highlight.blinks",
				Config.editor().selectionHighlight.blinks.value())
		);
	}
}

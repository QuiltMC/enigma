package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleCheckBoxItem;
import org.quiltmc.enigma.util.I18n;

import static org.quiltmc.enigma.gui.util.GuiUtil.syncStateWithConfig;

public class EntryTooltipsMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.entry_tooltips";

	private final SimpleCheckBoxItem enable = new SimpleCheckBoxItem("menu.view.entry_tooltips.enable");
	private final SimpleCheckBoxItem interactable = new SimpleCheckBoxItem("menu.view.entry_tooltips.interactable");

	protected EntryTooltipsMenu(Gui gui) {
		super(gui);

		syncStateWithConfig(this.enable, Config.editor().entryTooltips.enable);
		syncStateWithConfig(this.interactable, Config.editor().entryTooltips.interactable);

		this.add(this.enable);
		this.add(this.interactable);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.enable.retranslate();
		this.interactable.retranslate();
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

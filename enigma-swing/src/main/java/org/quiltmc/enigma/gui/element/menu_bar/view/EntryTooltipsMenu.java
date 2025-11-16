package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;

import static org.quiltmc.enigma.gui.util.GuiUtil.createSyncedMenuCheckBox;

public class EntryTooltipsMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view.entry_tooltips";

	private final JCheckBoxMenuItem enable = createSyncedMenuCheckBox(Config.editor().entryTooltips.enable);
	private final JCheckBoxMenuItem interactable = createSyncedMenuCheckBox(Config.editor().entryTooltips.interactable);

	protected EntryTooltipsMenu(Gui gui) {
		super(gui);

		this.add(this.enable);
		this.add(this.interactable);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.enable.setText(I18n.translate("menu.view.entry_tooltips.enable"));
		this.interactable.setText(I18n.translate("menu.view.entry_tooltips.interactable"));
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

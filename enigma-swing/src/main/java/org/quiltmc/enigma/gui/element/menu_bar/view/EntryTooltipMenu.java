package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;

import static org.quiltmc.enigma.gui.util.GuiUtil.createSyncedCheckBox;

public class EntryTooltipMenu extends AbstractEnigmaMenu {
	private final JCheckBoxMenuItem enable = createSyncedCheckBox(Config.editor().entryTooltip.enable);
	private final JCheckBoxMenuItem interactable = createSyncedCheckBox(Config.editor().entryTooltip.interactable);

	protected EntryTooltipMenu(Gui gui) {
		super(gui);

		this.add(this.enable);
		this.add(this.interactable);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.entry_tooltip"));
		this.enable.setText(I18n.translate("menu.view.entry_tooltip.enable"));
		this.interactable.setText(I18n.translate("menu.view.entry_tooltip.interactable"));
	}
}

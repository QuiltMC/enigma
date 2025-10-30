package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

public class EntryMarkersMenu extends AbstractEnigmaMenu {
	private final JCheckBoxMenuItem tooltip = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.tooltip);

	private final JMenu markMenu = new JMenu();
	private final JCheckBoxMenuItem onlyMarkDeclarations = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.onlyMarkDeclarations);
	private final JCheckBoxMenuItem markObfuscated = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markObfuscated);
	private final JCheckBoxMenuItem markFallback = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markFallback);
	private final JCheckBoxMenuItem markProposed = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markProposed);
	private final JCheckBoxMenuItem markDeobfuscated = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markDeobfuscated);

	public EntryMarkersMenu(Gui gui) {
		super(gui);

		this.add(this.tooltip);

		this.markMenu.add(this.onlyMarkDeclarations);
		this.markMenu.add(this.markObfuscated);
		this.markMenu.add(this.markFallback);
		this.markMenu.add(this.markProposed);
		this.markMenu.add(this.markDeobfuscated);

		this.add(this.markMenu);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.entry_markers"));

		this.tooltip.setText(I18n.translate("menu.view.entry_markers.tooltip"));

		this.markMenu.setText(I18n.translate("menu.view.entry_markers.mark"));

		this.onlyMarkDeclarations.setText(I18n.translate("menu.view.entry_markers.mark.only_declarations"));
		this.markObfuscated.setText(I18n.translate("menu.view.entry_markers.mark.obfuscated"));
		this.markFallback.setText(I18n.translate("menu.view.entry_markers.mark.fallback"));
		this.markProposed.setText(I18n.translate("menu.view.entry_markers.mark.proposed"));
		this.markDeobfuscated.setText(I18n.translate("menu.view.entry_markers.mark.deobfuscated"));
	}
}

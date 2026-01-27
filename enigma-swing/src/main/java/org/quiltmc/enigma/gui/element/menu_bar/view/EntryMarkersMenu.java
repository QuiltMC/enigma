package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleCheckBoxItem;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleEnigmaMenu;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JToolBar;

import static org.quiltmc.enigma.gui.config.EntryMarkersSection.MAX_MAX_MARKERS_PER_LINE;
import static org.quiltmc.enigma.gui.config.EntryMarkersSection.MIN_MAX_MARKERS_PER_LINE;

public class EntryMarkersMenu extends AbstractEnigmaMenu {
	private final SimpleCheckBoxItem tooltip = new SimpleCheckBoxItem("menu.view.entry_markers.tooltip");

	private final SimpleEnigmaMenu maxMarkersPerLineMenu;
	private final SimpleEnigmaMenu markMenu;

	private final SimpleCheckBoxItem onlyMarkDeclarations = new SimpleCheckBoxItem("menu.view.entry_markers.mark.only_declarations");
	private final SimpleCheckBoxItem markObfuscated = new SimpleCheckBoxItem("menu.view.entry_markers.mark.obfuscated");
	private final SimpleCheckBoxItem markFallback = new SimpleCheckBoxItem("menu.view.entry_markers.mark.fallback");
	private final SimpleCheckBoxItem markProposed = new SimpleCheckBoxItem("menu.view.entry_markers.mark.proposed");
	private final SimpleCheckBoxItem markDeobfuscated = new SimpleCheckBoxItem("menu.view.entry_markers.mark.deobfuscated");

	public EntryMarkersMenu(Gui gui) {
		super(gui);

		this.maxMarkersPerLineMenu = GuiUtil.createIntConfigRadioMenu(
			gui, "menu.view.entry_markers.max_markers_per_line",
			Config.editor().entryMarkers.maxMarkersPerLine,
			MIN_MAX_MARKERS_PER_LINE, MAX_MAX_MARKERS_PER_LINE
		);

		this.markMenu = new SimpleEnigmaMenu(gui, "menu.view.entry_markers.mark", I18n::translate);

		GuiUtil.syncStateWithConfig(this.tooltip, Config.editor().entryMarkers.tooltip);
		GuiUtil.syncStateWithConfig(this.onlyMarkDeclarations, Config.editor().entryMarkers.onlyMarkDeclarations);
		GuiUtil.syncStateWithConfig(this.markObfuscated, Config.editor().entryMarkers.markObfuscated);
		GuiUtil.syncStateWithConfig(this.markFallback, Config.editor().entryMarkers.markFallback);
		GuiUtil.syncStateWithConfig(this.markProposed, Config.editor().entryMarkers.markProposed);
		GuiUtil.syncStateWithConfig(this.markDeobfuscated, Config.editor().entryMarkers.markDeobfuscated);

		this.add(this.tooltip);

		this.add(this.maxMarkersPerLineMenu);

		this.markMenu.add(this.onlyMarkDeclarations);
		this.markMenu.add(new JToolBar.Separator());
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

		this.tooltip.retranslate();

		this.maxMarkersPerLineMenu.retranslate();
		this.markMenu.retranslate();

		this.onlyMarkDeclarations.retranslate();
		this.markObfuscated.retranslate();
		this.markFallback.retranslate();
		this.markProposed.retranslate();
		this.markDeobfuscated.retranslate();
	}
}

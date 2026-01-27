package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.FontDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleItem;
import org.quiltmc.enigma.util.I18n;

public class ViewMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.view";

	private final StatsMenu stats;
	private final NotificationsMenu notifications;
	private final LanguagesMenu languages;
	private final ThemesMenu themes;
	private final ScaleMenu scale;
	private final EntryTooltipsMenu entryTooltips;
	private final SelectionHighlightMenu selectionHighlight;
	private final EntryMarkersMenu entryMarkers;

	private final SimpleItem fontItem = new SimpleItem("menu.view.font");

	public ViewMenu(Gui gui) {
		super(gui);
		this.stats = new StatsMenu(gui);
		this.notifications = new NotificationsMenu(gui);
		this.languages = new LanguagesMenu(gui);
		this.themes = new ThemesMenu(gui);
		this.scale = new ScaleMenu(gui);
		this.entryTooltips = new EntryTooltipsMenu(gui);
		this.selectionHighlight = new SelectionHighlightMenu(gui);
		this.entryMarkers = new EntryMarkersMenu(gui);

		this.add(this.themes);
		this.add(this.selectionHighlight);
		this.add(this.languages);
		this.add(this.notifications);
		this.add(this.scale);
		this.add(this.stats);
		this.add(this.entryTooltips);
		this.add(this.entryMarkers);
		this.add(this.fontItem);

		this.fontItem.addActionListener(e -> this.onFontClicked(this.gui));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));

		this.themes.retranslate();
		this.notifications.retranslate();
		this.languages.retranslate();
		this.scale.retranslate();
		this.stats.retranslate();
		this.entryTooltips.retranslate();
		this.selectionHighlight.retranslate();
		this.entryMarkers.retranslate();
		this.fontItem.retranslate();
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.stats.updateState(jarOpen, state);
		this.notifications.updateState(jarOpen, state);
		this.languages.updateState(jarOpen, state);
		this.scale.updateState(jarOpen, state);
	}

	private void onFontClicked(Gui gui) {
		FontDialog.display(gui.getFrame());
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

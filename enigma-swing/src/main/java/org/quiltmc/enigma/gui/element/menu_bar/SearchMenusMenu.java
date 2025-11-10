package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JPanel;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	final PlaceheldTextField field = new PlaceheldTextField();
	final JPanel results = new JPanel();

	protected SearchMenusMenu(Gui gui) {
		super(gui);

		this.add(this.field);
		this.add(this.results);

		// TODO focus field on open

		// TODO KeyBinds: up/down -> prev/next result, enter -> doClick on selected result

		this.retranslate();
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		// TODO check any caching
	}

	@Override
	public void retranslate() {
		// TODO check any caching
		this.setText(I18n.translate("menu.help.search"));
		this.field.setPlaceholder(I18n.translate("menu.help.search.placeholder"));
	}
}

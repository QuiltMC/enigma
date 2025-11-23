package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;

public abstract class AbstractSearchableEnigmaMenu extends AbstractEnigmaMenu implements ConventionalSearchableElement {
	protected AbstractSearchableEnigmaMenu(Gui gui) {
		super(gui);
	}

	@Override
	public String getSearchName() {
		return this.getText();
	}

	@Override
	public void onSearchChosen() {
		this.doClick(0);
	}
}

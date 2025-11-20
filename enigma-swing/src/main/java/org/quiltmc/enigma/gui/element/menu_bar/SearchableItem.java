package org.quiltmc.enigma.gui.element.menu_bar;

import javax.swing.JMenuItem;

public class SearchableItem extends JMenuItem implements SearchableElement {
	private final String aliasesTranslationKeyPrefix;

	public SearchableItem(String aliasesTranslationKeyPrefix) {
		this.aliasesTranslationKeyPrefix = aliasesTranslationKeyPrefix;
	}

	@Override
	public String getSearchName() {
		return this.getText();
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return this.aliasesTranslationKeyPrefix;
	}

	@Override
	public void onSearchClicked() {
		this.doClick();
	}
}

package org.quiltmc.enigma.gui.element.menu_bar;

import javax.swing.JCheckBoxMenuItem;

public class SearchableCheckBoxItem extends JCheckBoxMenuItem implements SearchableElement {
	private final String aliasesTranslationKeyPrefix;

	public SearchableCheckBoxItem(String aliasesTranslationKeyPrefix) {
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

package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenuItem;

public class SimpleItem extends JMenuItem implements SearchableElement, Retranslatable {
	private final String translationKey;

	public SimpleItem(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(this.getAliasesTranslationKeyPrefix()));
	}

	@Override
	public String getSearchName() {
		return this.getText();
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return this.translationKey;
	}

	@Override
	public void onSearchClicked() {
		this.doClick();
	}
}

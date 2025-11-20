package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;

public class SimpleCheckBoxItem extends JCheckBoxMenuItem implements SearchableElement, Retranslatable {
	private final String translationKey;

	public SimpleCheckBoxItem(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(this.translationKey));
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

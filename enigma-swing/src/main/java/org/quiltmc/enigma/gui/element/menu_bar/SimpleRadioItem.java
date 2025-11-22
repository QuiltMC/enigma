package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

import javax.swing.JRadioButtonMenuItem;

public class SimpleRadioItem extends JRadioButtonMenuItem implements ConventionalSearchableElement, Retranslatable {
	private final String translationKey;

	public SimpleRadioItem(String translationKey) {
		this.translationKey = translationKey;
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

	@Override
	public void retranslate() {
		this.setText(I18n.translate(this.translationKey));
	}
}

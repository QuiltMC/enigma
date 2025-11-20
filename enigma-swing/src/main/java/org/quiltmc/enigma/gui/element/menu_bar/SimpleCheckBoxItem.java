package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

public class SimpleCheckBoxItem extends SearchableCheckBoxItem implements Retranslatable {
	public SimpleCheckBoxItem(String translationKey) {
		super(translationKey);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(this.getAliasesTranslationKeyPrefix()));
	}
}

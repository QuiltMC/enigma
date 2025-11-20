package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

public class SimpleItem extends SearchableItem implements Retranslatable {
	public SimpleItem(String translationKey) {
		super(translationKey);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(this.getAliasesTranslationKeyPrefix()));
	}
}

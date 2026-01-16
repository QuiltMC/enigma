package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;

import java.util.function.UnaryOperator;

public class SimpleEnigmaMenu extends AbstractSearchableEnigmaMenu {
	protected final String translationKey;
	protected final UnaryOperator<String> retranslate;

	public SimpleEnigmaMenu(Gui gui, String translationKey, UnaryOperator<String> retranslate) {
		super(gui);

		this.translationKey = translationKey;
		this.retranslate = retranslate;
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return this.translationKey;
	}

	@Override
	public void retranslate() {
		this.setText(this.retranslate.apply(this.translationKey));
	}
}

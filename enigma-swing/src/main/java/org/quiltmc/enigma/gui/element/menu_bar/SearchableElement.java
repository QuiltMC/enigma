package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

import javax.swing.MenuElement;
import java.util.Arrays;
import java.util.stream.Stream;

public interface SearchableElement extends MenuElement {
	String ALIAS_DELIMITER = ";";

	static Stream<String> translateExtraAliases(String translationKey) {
		final String aliases = I18n.translateOrNull(translationKey);

		return aliases == null ? Stream.empty() : Arrays.stream(aliases.split(ALIAS_DELIMITER));
	}

	Stream<String> streamSearchAliases();

	String getSearchName();

	void onSearchChosen();
}

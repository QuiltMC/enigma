package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.util.I18n;

import javax.swing.MenuElement;
import java.util.Arrays;
import java.util.stream.Stream;

public interface SearchableElement extends MenuElement {
	default Stream<String> streamSearchAliases() {
		final String aliases = I18n
				.translateOrNull(this.getAliasesTranslationKeyPrefix() + ".aliases");

		return Stream.concat(
			Stream.of(this.getSearchName()),
			aliases == null ? Stream.empty() : Arrays.stream(aliases.split(";"))
		);
	}

	String getSearchName();

	String getAliasesTranslationKeyPrefix();

	void onSearchClicked();
}

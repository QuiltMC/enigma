package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.util.I18n;

import javax.swing.MenuElement;
import java.util.Arrays;
import java.util.stream.Stream;

public interface SearchableElement extends MenuElement {

	String ALIASES_SUFFIX = ".aliases";
	String ALIAS_DELIMITER = ";";

	default Stream<String> streamSearchAliases() {
		final String aliases = I18n
				.translateOrNull(this.getAliasesTranslationKeyPrefix() + ALIASES_SUFFIX);

		return Stream.concat(
			Stream.of(this.getSearchName()),
			aliases == null ? Stream.empty() : Arrays.stream(aliases.split(ALIAS_DELIMITER))
		);
	}

	String getSearchName();

	/**
	 * Returns a translation key prefix used to retrieve translatable search aliases.<br>
	 * Usually the prefix is the translation key of the translatable element.
	 *
	 * <p> {@value ALIASES_SUFFIX} is appended to create the complete translation key.<br>
	 * Alias translations hold multiple aliases separated by {@value ALIAS_DELIMITER}.
	 *
	 * <p> <em>All</em> alias translation key prefixes should be documented in {@code CONTRIBUTING.md} under<br>
	 * {@code Translating > Search Aliases > Complete list of search alias translation keys}
	 */
	String getAliasesTranslationKeyPrefix();

	void onSearchClicked();
}

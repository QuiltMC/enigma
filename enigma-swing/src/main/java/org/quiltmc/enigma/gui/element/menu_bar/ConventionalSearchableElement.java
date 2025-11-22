package org.quiltmc.enigma.gui.element.menu_bar;

import java.util.stream.Stream;

public interface ConventionalSearchableElement extends SearchableElement {
	String ALIASES_SUFFIX = ".aliases";

	@Override
	default Stream<String> streamSearchAliases() {
		return Stream.concat(
			Stream.of(this.getSearchName()),
			SearchableElement.translateExtraAliases(this.getAliasesTranslationKeyPrefix() + ALIASES_SUFFIX)
		);
	}

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
}

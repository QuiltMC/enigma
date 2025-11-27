package org.quiltmc.enigma.gui.element.menu_bar;

import java.util.stream.Stream;

/**
 * A {@link SearchableElement} that loads {@linkplain #streamSearchAliases() search aliases} from translations using
 * {@link #translateExtraAliases(String)}.
 *
 * <p> Aliases are loaded from the translation key formed by appending {@value ALIASES_SUFFIX} to the
 * {@linkplain #getAliasesTranslationKeyPrefix() key prefix}.<br>
 */
public interface ConventionalSearchableElement extends SearchableElement {
	String ALIASES_SUFFIX = ".aliases";

	@Override
	default Stream<String> streamSearchAliases() {
		return Stream.concat(
			SearchableElement.super.streamSearchAliases(),
			SearchableElement.translateExtraAliases(this.getAliasesTranslationKeyPrefix() + ALIASES_SUFFIX)
		);
	}

	/**
	 * Returns a translation key prefix used to retrieve translatable search aliases.<br>
	 * Usually the prefix is the translation key of the translatable element.
	 */
	String getAliasesTranslationKeyPrefix();
}

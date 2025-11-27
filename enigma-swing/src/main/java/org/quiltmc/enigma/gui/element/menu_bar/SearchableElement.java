package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.util.I18n;

import javax.swing.AbstractButton;
import javax.swing.MenuElement;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A menu element that can appear as a search result in {@link SearchMenusMenu}.
 *
 * <p> <em>All</em> alias translation keys passed to {@link #translateExtraAliases(String)} should be documented in
 * {@code CONTRIBUTING.md} under<br>
 * {@code Translating > Search Aliases > Complete list of search alias translation keys}<br>
 * Alias translations are often absent from language files, so keeping the table populated is essential for
 * discoverability for translators.
 *
 * <p> {@link ConventionalSearchableElement} adds a convention for obtaining
 * {@linkplain #streamSearchAliases() search aliases} using {@link #translateExtraAliases(String)}.
 *
 * @see SimpleItem
 * @see SimpleCheckBoxItem
 * @see SimpleRadioItem
 */
public interface SearchableElement extends MenuElement {
	String ALIAS_DELIMITER = ";";

	/**
	 * Loads a {@value #ALIAS_DELIMITER}-separated list of aliases from the passed {@code translationKey}.
	 *
	 * <p> Having no entry for {@code translationKey} in a language file is common;
	 * it means the element has no aliases in that language.
	 *
	 * @see ConventionalSearchableElement
	 */
	static Stream<String> translateExtraAliases(String translationKey) {
		final String aliases = I18n.translateOrNull(translationKey);

		return aliases == null ? Stream.empty() : Arrays.stream(aliases.split(ALIAS_DELIMITER));
	}

	/**
	 * @return a finite, ordered {@link Stream} of aliases that this element can be searched by
	 *
	 * @implSpec the {@linkplain #getSearchName() search name} should always be included first
	 * (unless this element is not currently searchable, in which case an empty stream may be returned)
	 */
	default Stream<String> streamSearchAliases() {
		return Stream.of(this.getSearchName());
	}

	/**
	 * @return the name of this element uses in search results; usually {@link AbstractButton#getText()}
	 */
	String getSearchName();

	/**
	 * Called when this element's search result is chosen.<br>
	 * Most implementations call {@link AbstractButton#doClick(int) doClick(0)}
	 */
	void onSearchChosen();
}

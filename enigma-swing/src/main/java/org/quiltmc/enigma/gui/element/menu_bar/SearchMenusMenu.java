package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.element.SearchableElement;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;

import javax.swing.JPanel;
import javax.swing.MenuElement;
import java.util.Arrays;
import java.util.stream.Stream;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	/**
	 * @return a breadth-first stream of the passed {@code root} element and all of its sub-elements
	 */
	private static Stream<MenuElement> streamElementTree(MenuElement root) {
		return Stream.concat(
				Stream.of(root),
				Arrays.stream(root.getSubElements()).flatMap(SearchMenusMenu::streamElementTree)
		);
	}

	private final PlaceheldTextField field = new PlaceheldTextField();
	private final JPanel results = new JPanel();

	private StringMultiTrie.View<SearchableElement> elements;

	protected SearchMenusMenu(Gui gui) {
		super(gui);

		this.add(this.field);
		this.add(this.results);

		SearchMenusMenu.this.field.addHierarchyListener(e -> {
			if (SearchMenusMenu.this.field.isShowing()) {
				SearchMenusMenu.this.field.requestFocus();
				SearchMenusMenu.this.field.selectAll();
			}
		});

		// TODO KeyBinds: up/down -> prev/next result, enter -> doClick on selected result

		this.retranslate();
	}

	private StringMultiTrie.View<SearchableElement> getElements() {
		if (this.elements == null) {
			this.elements = this.buildElementsTrie();
		}

		return this.elements;
	}

	private void clearElements() {
		this.elements = null;
	}

	private StringMultiTrie.View<SearchableElement> buildElementsTrie() {
		final CompositeStringMultiTrie<SearchableElement> elementsBuilder = CompositeStringMultiTrie.createHashed();
		this.gui.getMenuBar()
				.streamMenus()
				.flatMap(SearchMenusMenu::streamElementTree)
				.<SearchableElement>mapMulti((element, keep) -> {
					if (element instanceof SearchableElement searchable) {
						keep.accept(searchable);
					}
				})
				.forEach(searchable -> searchable
					.streamSearchAliases()
					.forEach(alias -> elementsBuilder.put(alias, searchable))
				);

		return elementsBuilder.getView();
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.clearElements();
	}

	@Override
	public void retranslate() {
		this.clearElements();
		this.setText(I18n.translate("menu.help.search"));
		this.field.setPlaceholder(I18n.translate("menu.help.search.placeholder"));
	}
}

package org.quiltmc.enigma.gui.element.menu_bar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.element.SearchableElement;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.CharacterNode;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	/**
	 * @return a breadth-first stream of the passed {@code root} element and all of its sub-elements,
	 * excluding {@link SearchMenusMenu}s and their sub-elements
	 */
	private static Stream<MenuElement> streamElementTree(MenuElement root) {
		return root instanceof SearchMenusMenu ? Stream.empty() : Stream.concat(
				Stream.of(root),
				Arrays.stream(root.getSubElements()).flatMap(SearchMenusMenu::streamElementTree)
		);
	}

	private final PlaceheldTextField field = new PlaceheldTextField();
	private final JMenuItem noResults = new JMenuItem();

	private final ResultManager resultManager = new ResultManager();

	protected SearchMenusMenu(Gui gui) {
		super(gui);

		this.noResults.setEnabled(false);
		this.noResults.setVisible(false);

		this.add(this.field);
		this.add(this.noResults);

		// Always focus field, but don't always select its text, because it loses focus when packing new search results.
		this.field.addHierarchyListener(e -> {
			if (this.field.isShowing()) {
				this.field.requestFocus();
			}
		});

		// Only select field text when the menu is selected, so text isn't selected when packing new search results.
		this.addMenuListener(new MenuListener() {
			final HierarchyListener fieldTextSelector = new HierarchyListener() {
				@Override
				public void hierarchyChanged(HierarchyEvent e) {
					if (SearchMenusMenu.this.field.isShowing()) {
						SearchMenusMenu.this.field.removeHierarchyListener(this);

						SearchMenusMenu.this.field.selectAll();
					}
				}
			};

			@Override
			public void menuSelected(MenuEvent e) {
				SearchMenusMenu.this.field.addHierarchyListener(this.fieldTextSelector);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				SearchMenusMenu.this.field.removeHierarchyListener(this.fieldTextSelector);
			}

			@Override
			public void menuCanceled(MenuEvent e) {
				SearchMenusMenu.this.field.removeHierarchyListener(this.fieldTextSelector);
			}
		});

		this.field.getDocument().addDocumentListener(new DocumentListener() {
			void updateResultItems() {
				final String searchTerm = SearchMenusMenu.this.field.getText();

				if (searchTerm.isEmpty()) {
					SearchMenusMenu.this.noResults.setVisible(false);
					SearchMenusMenu.this.resultManager.clearCurrent();

					SearchMenusMenu.this.getPopupMenu().pack();
				} else {
					switch (SearchMenusMenu.this.resultManager.updateResultItems(searchTerm)) {
						case NO_RESULTS -> {
							SearchMenusMenu.this.noResults.setVisible(true);

							SearchMenusMenu.this.getPopupMenu().pack();
						}
						case SAME_RESULTS -> { }
						case DIFFERENT_RESULTS -> {
							SearchMenusMenu.this.noResults.setVisible(false);

							SearchMenusMenu.this.getPopupMenu().pack();
						}
					}
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				this.updateResultItems();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				this.updateResultItems();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				this.updateResultItems();
			}
		});

		// TODO KeyBinds: up/down -> prev/next result, enter -> doClick on selected result

		this.retranslate();
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.resultManager.clear();
	}

	@Override
	public void retranslate() {
		this.resultManager.clear();

		this.setText(I18n.translate("menu.help.search"));
		this.field.setPlaceholder(I18n.translate("menu.help.search.placeholder"));
		this.noResults.setText(I18n.translate("menu.help.search.no_results"));
	}

	private static class Result {
		final SearchableElement element;

		@Nullable JMenuItem item;

		Result(SearchableElement element) {
			this.element = element;
		}

		JMenuItem getItem() {
			if (this.item == null) {
				this.item = new JMenuItem(this.element.getSearchName());
			}

			return this.item;
		}
	}

	private class ResultManager {
		StringMultiTrie.@Nullable View<Result, ?, ?> resultTrie;
		@Nullable
		CurrentResults currentResults;

		/**
		 * @return {@code true} if there are any results, or {@code false} otherwise
		 */
		UpdateOutcome updateResultItems(String searchTerm) {
			if (this.currentResults == null || !searchTerm.startsWith(this.currentResults.searchTerm)) {
				return this.initializeCurrentResults(searchTerm);
			} else {
				if (this.currentResults.searchTerm.length() == searchTerm.length()) {
					return UpdateOutcome.SAME_RESULTS;
				} else {
					CharacterNode<Result> resultNode = this.currentResults.results;
					for (int i = this.currentResults.searchTerm.length(); i < searchTerm.length(); i++) {
						resultNode = resultNode.nextIgnoreCase(searchTerm.charAt(i));
					}

					if (resultNode.isEmpty()) {
						this.clearCurrent();

						return UpdateOutcome.NO_RESULTS;
					} else {
						final Set<Result> newResults = resultNode.streamValues().collect(Collectors.toSet());

						final Set<JMenuItem> excludedResults = this.currentResults.results.streamValues()
								.filter(oldResult -> !newResults.contains(oldResult))
								.map(Result::getItem)
								.collect(Collectors.toSet());

						if (excludedResults.isEmpty()) {
							return UpdateOutcome.SAME_RESULTS;
						} else {
							excludedResults
								.forEach(SearchMenusMenu.this::remove);

							this.currentResults = new CurrentResults(resultNode, searchTerm);

							return UpdateOutcome.DIFFERENT_RESULTS;
						}
					}
				}
			}
		}

		UpdateOutcome initializeCurrentResults(String searchTerm) {
			final CharacterNode<Result> results = this.getResultTrie().getIgnoreCase(searchTerm);
			if (results.isEmpty()) {
				this.clearCurrent();

				return UpdateOutcome.NO_RESULTS;
			} else {
				this.currentResults = new CurrentResults(results, searchTerm);
				this.currentResults.results.streamValues().map(Result::getItem).forEach(SearchMenusMenu.this::add);

				return UpdateOutcome.DIFFERENT_RESULTS;
			}
		}

		StringMultiTrie.View<Result, ?, ?> getResultTrie() {
			if (this.resultTrie == null) {
				this.resultTrie = this.buildResultTrie();
			}

			return this.resultTrie;
		}

		void clear() {
			this.resultTrie = null;
			this.clearCurrent();
		}

		void clearCurrent() {
			if (this.currentResults != null) {
				this.currentResults.results.streamValues()
						.map(Result::getItem)
						.forEach(SearchMenusMenu.this::remove);

				this.currentResults = null;
			}
		}

		StringMultiTrie.View<Result, ?, ?> buildResultTrie() {
			final CompositeStringMultiTrie<Result> elementsBuilder = CompositeStringMultiTrie.createHashed();
			SearchMenusMenu.this.gui.getMenuBar()
					.streamMenus()
					.flatMap(SearchMenusMenu::streamElementTree)
					.<SearchableElement>mapMulti((element, keep) -> {
						if (element instanceof SearchableElement searchable) {
							keep.accept(searchable);
						}
					})
					.map(Result::new)
					.forEach(result -> result
						.element.streamSearchAliases()
						.forEach(alias -> elementsBuilder.put(alias, result))
					);

			return elementsBuilder.getView();
		}

		record CurrentResults(CharacterNode<Result> results, String searchTerm) { }

		enum UpdateOutcome {
			NO_RESULTS, SAME_RESULTS, DIFFERENT_RESULTS
		}
	}
}

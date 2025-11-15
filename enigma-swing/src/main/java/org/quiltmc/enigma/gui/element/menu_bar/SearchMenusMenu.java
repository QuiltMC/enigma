package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.element.SearchableElement;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;

import javax.annotation.Nullable;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

		this.field.addHierarchyListener(e -> {
			if (this.field.isShowing()) {
				this.field.requestFocus();
				this.field.selectAll();
			}
		});

		this.field.getDocument().addDocumentListener(new DocumentListener() {
			void updateResultItems() {
				final String searchTerm = SearchMenusMenu.this.field.getText();

				if (searchTerm.isEmpty()) {
					SearchMenusMenu.this.noResults.setVisible(false);
					SearchMenusMenu.this.invalidate();
					SearchMenusMenu.this.repaint();
					SearchMenusMenu.this.resultManager.clearCurrent();
				} else {
					switch (SearchMenusMenu.this.resultManager.updateResultItems(searchTerm)) {
						case NO_RESULTS -> {
							SearchMenusMenu.this.noResults.setVisible(false);

							SearchMenusMenu.this.getPopupMenu().pack();
							SearchMenusMenu.this.getPopupMenu().pack();
						}
						case SAME_RESULTS -> { }
						case DIFFERENT_RESULTS -> {
							SearchMenusMenu.this.noResults.setVisible(true);

							SearchMenusMenu.this.getPopupMenu().pack();
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
	}

	private static class Result {
		final SearchableElement element;
		final String alias;

		@Nullable JMenuItem item;

		Result(SearchableElement element, String alias) {
			this.element = element;
			this.alias = alias;
		}

		JMenuItem getItem() {
			if (this.item == null) {
				this.item = new JMenuItem(this.alias);
			}

			return this.item;
		}
	}

	private class ResultManager {
		@Nullable
		StringMultiTrie.View<Result> resultTrie;
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
					MultiTrie.Node<Character, Result> resultNode = this.currentResults.results;
					for (int i = this.currentResults.searchTerm.length(); i < searchTerm.length(); i++) {
						resultNode = resultNode.next(searchTerm.charAt(i));
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
			final MultiTrie.Node<Character, Result> results = this.getResultTrie().get(searchTerm);
			if (results.isEmpty()) {
				this.clearCurrent();

				return UpdateOutcome.NO_RESULTS;
			} else {
				this.currentResults = new CurrentResults(results, searchTerm);
				this.currentResults.results.streamValues().map(Result::getItem).forEach(SearchMenusMenu.this::add);

				return UpdateOutcome.DIFFERENT_RESULTS;
			}
		}

		StringMultiTrie.View<Result> getResultTrie() {
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

		StringMultiTrie.View<Result> buildResultTrie() {
			final CompositeStringMultiTrie<Result> elementsBuilder = CompositeStringMultiTrie.createHashed();
			SearchMenusMenu.this.gui.getMenuBar()
					.streamMenus()
					.flatMap(SearchMenusMenu::streamElementTree)
					.<SearchableElement>mapMulti((element, keep) -> {
						if (element instanceof SearchableElement searchable) {
							keep.accept(searchable);
						}
					})
					.forEach(searchable -> searchable
						.streamSearchAliases()
						.forEach(alias -> elementsBuilder.put(alias, new Result(searchable, alias)))
					);

			return elementsBuilder.getView();
		}

		record CurrentResults(MultiTrie.Node<Character, Result> results, String searchTerm) { }

		enum UpdateOutcome {
			NO_RESULTS, SAME_RESULTS, DIFFERENT_RESULTS
		}
	}
}

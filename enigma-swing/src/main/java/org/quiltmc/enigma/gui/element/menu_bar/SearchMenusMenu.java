package org.quiltmc.enigma.gui.element.menu_bar;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.EmptyStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.Node;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	@Nullable
	private final Border defaultPopupBorder;

	/**
	 * @return a breadth-first stream of the passed {@code root} element and all of its sub-elements,
	 * excluding the {@link HelpMenu} and its sub-elements; the help menu is not searchable because it must be open
	 * to start searching in the first place
	 */
	private static Stream<MenuElement> streamElementTree(MenuElement root) {
		return root instanceof HelpMenu ? Stream.empty() : Stream.concat(
				Stream.of(root),
				Arrays.stream(root.getSubElements()).flatMap(SearchMenusMenu::streamElementTree)
		);
	}

	private final PlaceheldTextField field = new PlaceheldTextField();
	private final JMenuItem noResults = new JMenuItem();

	@Nullable
	private Lookup lookup;

	protected SearchMenusMenu(Gui gui) {
		super(gui);

		this.defaultPopupBorder = this.getPopupMenu().getBorder();

		this.noResults.setEnabled(false);
		this.noResults.setVisible(false);

		this.addPermanentChildren();

		// Always focus field, but don't always select its text, because it loses focus when packing new search results.
		this.field.addHierarchyListener(e -> {
			if (this.field.isShowing()) {
				this.field.requestFocus();
			}
		});

		this.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SearchMenusMenu.this.field.selectAll();

				SearchMenusMenu.this.updateResultItems();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) { }
		});

		this.field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				SearchMenusMenu.this.updateResultItems();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				SearchMenusMenu.this.updateResultItems();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				SearchMenusMenu.this.updateResultItems();
			}
		});

		// TODO KeyBinds: enter -> doClick on selected result

		this.retranslate();
	}

	private void updateResultItems() {
		final String searchTerm = this.field.getText();

		final Results results = this.getLookup().search(searchTerm);

		if (results instanceof Results.None) {
			this.keepOnlyPermanentChildren();

			this.noResults.setVisible(!searchTerm.isEmpty());

			this.refreshPopup();
		} else if (results instanceof Results.Different different) {
			this.keepOnlyPermanentChildren();

			this.noResults.setVisible(different.results.isEmpty());

			different.results.forEach(this::add);

			this.refreshPopup();
		} // else Results.Same
	}

	private void refreshPopup() {
		// HACK: When popups are resizing in limited space, they may remove their borders.
		// The border won't be restored when re-packing or showing, so manually restore the original border here.
		this.getPopupMenu().setBorder(this.defaultPopupBorder);
		this.getPopupMenu().pack();

		final Point popupMenuOrigin = this.getPopupMenuOrigin();
		this.getPopupMenu().show(this, popupMenuOrigin.x, popupMenuOrigin.y);
	}

	private void addPermanentChildren() {
		this.add(this.field);
		this.add(this.noResults);
	}

	private void keepOnlyPermanentChildren() {
		this.removeAll();
		this.addPermanentChildren();
	}

	private Lookup getLookup() {
		if (this.lookup == null) {
			this.lookup = Lookup.build(this.gui);
		}

		return this.lookup;
	}

	public void clearLookup() {
		this.lookup = null;
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.clearLookup();
	}

	@Override
	public void retranslate() {
		this.clearLookup();

		this.setText(I18n.translate("menu.help.search"));
		this.field.setPlaceholder(I18n.translate("menu.help.search.placeholder"));
		this.noResults.setText(I18n.translate("menu.help.search.no_results"));
	}

	private static final class Lookup {
		static final int MAX_SUBSTRING_LENGTH = 2;

		final ResultCache emptyCache = new ResultCache("", EmptyStringMultiTrie.Node.get(), ImmutableList.of());

		static int getCommonPrefixLength(String left, String right) {
			final int minLength = Math.min(left.length(), right.length());

			for (int i = 0; i < minLength; i++) {
				if (left.charAt(i) != right.charAt(i)) {
					return i;
				}
			}

			return minLength;
		}

		static Lookup build(Gui gui) {
			final CompositeStringMultiTrie<Result> prefixBuilder = CompositeStringMultiTrie.createHashed();
			final CompositeStringMultiTrie<Result> substringBuilder = CompositeStringMultiTrie.createHashed();
			gui.getMenuBar()
					.streamMenus()
					.flatMap(SearchMenusMenu::streamElementTree)
					.<SearchableElement>mapMulti((element, keep) -> {
						if (element instanceof SearchableElement searchable) {
							keep.accept(searchable);
						}
					})
					.map(Result::new)
					.forEach(result -> result.lowerCaseAliases.forEach(alias -> {
						prefixBuilder.put(alias, result);

						final int aliasLength = alias.length();
						for (int start = 1; start < aliasLength; start++) {
							final int end = Math.min(start + MAX_SUBSTRING_LENGTH, aliasLength);
							MutableStringMultiTrie.Node<Result> node = substringBuilder.getRoot();
							for (int i = start; i < end; i++) {
								node = node.next(alias.charAt(i));
							}

							node.put(result);
						}
					}));

			return new Lookup(prefixBuilder.view(), substringBuilder.view());
		}

		// maps complete search aliases to their corresponding results
		final StringMultiTrie<Result> prefixResults;
		// maps all non-prefix MAX_SUBSTRING_LENGTH-length (or less) substrings of search
		// aliases to their corresponding result; used to narrow down the search scope for substring matches
		final StringMultiTrie<Result> substringResults;

		@NonNull
		ResultCache resultCache = this.emptyCache;

		Lookup(StringMultiTrie<Result> prefixResults, StringMultiTrie<Result> substringResults) {
			this.prefixResults = prefixResults;
			this.substringResults = substringResults;
		}

		Results search(String term) {
			if (term.isEmpty()) {
				final boolean wasEmpty = !this.resultCache.hasResults();

				this.resultCache = this.emptyCache;

				return wasEmpty ? Results.Same.INSTANCE : Results.None.INSTANCE;
			}

			final ResultCache oldCache = this.resultCache;
			this.resultCache = this.resultCache.updated(term.toLowerCase());

			if (this.resultCache.hasResults()) {
				if (this.resultCache.hasSameResults(oldCache)) {
					return Results.Same.INSTANCE;
				} else {
					return Results.Different.of(this.resultCache);
				}
			} else {
				return Results.None.INSTANCE;
			}
		}

		final class ResultCache {
			final String term;
			final Node<Result> prefixNode;
			final ImmutableList<Result> containingResults;

			ResultCache(
					String term, Node<Result> prefixNode, ImmutableList<Result> containingResults
			) {
				this.term = term;
				this.prefixNode = prefixNode;
				this.containingResults = containingResults;
			}

			boolean hasResults() {
				return !this.prefixNode.isEmpty() || !this.containingResults.isEmpty();
			}

			boolean hasSameResults(ResultCache other) {
				return this == other
						|| this.prefixNode == other.prefixNode
							&& this.containingResults.equals(other.containingResults);
			}

			ResultCache updated(String term) {
				if (this.term.isEmpty()) {
					return this.createFresh(term);
				} else {
					final int commonPrefixLength = getCommonPrefixLength(this.term, term);
					final int termLength = term.length();
					final int thisTermLength = this.term.length();

					if (commonPrefixLength == 0) {
						return this.createFresh(term);
					} else if (commonPrefixLength == termLength && commonPrefixLength == thisTermLength) {
						return this;
					} else {
						Node<Result> prefixNode = this.prefixNode.previous(thisTermLength - commonPrefixLength);
						if (termLength > commonPrefixLength) {
							for (int i = commonPrefixLength; i < termLength; i++) {
								prefixNode = prefixNode.next(term.charAt(i));

								if (prefixNode.isEmpty()) {
									break;
								}
							}
						}

						final ImmutableList<Result> containingResults;
						if (termLength > thisTermLength && termLength > MAX_SUBSTRING_LENGTH) {
							containingResults = this.containingResults.stream()
								.filter(result -> result.anyLowerCaseAliasContains(term))
								.collect(toImmutableList());
						} else {
							containingResults = this.buildContaining(term);
						}

						return new ResultCache(term, prefixNode, containingResults);
					}
				}
			}

			ResultCache createFresh(String term) {
				return new ResultCache(term, Lookup.this.prefixResults.get(term), this.buildContaining(term));
			}

			ImmutableList<Result> buildContaining(String term) {
				final int termLength = term.length();
				final List<Result> possibleResults = new ArrayList<>();
				// start at 1 because prefixes are handled by prefixTrie
				for (int start = 1; start <= termLength; start++) {
					final int end = Math.min(start + MAX_SUBSTRING_LENGTH, termLength);
					Node<Result> node = Lookup.this.substringResults.getRoot();
					for (int i = start; i < end; i++) {
						node = node.next(term.charAt(i));

						if (node.isEmpty()) {
							break;
						}
					}

					node.streamValues().forEach(possibleResults::add);
				}

				return possibleResults.stream()
					.distinct()
					.filter(result -> result.anyLowerCaseAliasContains(term))
					.collect(toImmutableList());
			}
		}
	}

	private static class Result implements Comparable<Result> {
		final SearchableElement element;

		final ImmutableList<String> lowerCaseAliases;

		// TODO display alias in item (if not search name)
		@Nullable JMenuItem item;

		Result(SearchableElement element) {
			this.element = element;
			this.lowerCaseAliases = this.element.streamSearchAliases()
				.filter(alias -> !alias.isEmpty())
				.map(String::toLowerCase)
				.collect(toImmutableList());
		}

		JMenuItem getItem() {
			if (this.item == null) {
				this.item = new JMenuItem(this.element.getSearchName());
			}

			return this.item;
		}

		boolean anyLowerCaseAliasContains(String term) {
			return this.lowerCaseAliases.stream().anyMatch(alias -> alias.contains(term));
		}

		@Override
		public int compareTo(@NonNull Result other) {
			return this.element.getSearchName().compareTo(other.element.getSearchName());
		}
	}

	private sealed interface Results {
		final class None implements Results {
			static final None INSTANCE = new None();
		}

		final class Same implements Results {
			static final Same INSTANCE = new Same();
		}

		record Different(ImmutableList<Component> results) implements Results {
			static Different of(Lookup.ResultCache cache) {
				return new Different(Stream
					.concat(cache.prefixNode.streamValues().sorted(), cache.containingResults.stream().sorted())
					.map(Result::getItem)
					.collect(toImmutableList())
				);
			}
		}
	}
}

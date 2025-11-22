package org.quiltmc.enigma.gui.element.menu_bar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.util.GuiUtil;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

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
		static final int NON_PREFIX_START = 1;
		static final int MAX_SUBSTRING_LENGTH = 2;

		final ResultCache emptyCache = new ResultCache(
				"", EmptyStringMultiTrie.Node.get(),
				ImmutableMap.of(), ImmutableSet.of()
		);

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
			final CompositeStringMultiTrie<Result> containingBuilder =
					CompositeStringMultiTrie.createHashed();
			gui.getMenuBar()
					.streamMenus()
					.flatMap(SearchMenusMenu::streamElementTree)
					.<SearchableElement>mapMulti((element, keep) -> {
						if (element instanceof SearchableElement searchable) {
							keep.accept(searchable);
						}
					})
					.forEach(element -> {
						final ImmutableMap<String, String> aliasesByLowercase = element.streamSearchAliases()
								.filter(alias -> !alias.isEmpty())
								.collect(toImmutableMap(
									String::toLowerCase,
									Function.identity(),
									// ignore case-insensitive duplicate aliases
									(left, right) -> left
								));

						final Result result = new Result(element, aliasesByLowercase);

						aliasesByLowercase.keySet().forEach(alias -> {
							prefixBuilder.put(alias, result);

							final int aliasLength = alias.length();
							for (int start = NON_PREFIX_START; start < aliasLength; start++) {
								final int end = Math.min(start + MAX_SUBSTRING_LENGTH, aliasLength);
								MutableStringMultiTrie.Node<Result> node = containingBuilder.getRoot();
								for (int i = start; i < end; i++) {
									node = node.next(alias.charAt(i));
								}

								node.put(result);
							}
						});
					});

			return new Lookup(prefixBuilder.view(), containingBuilder.view());
		}

		// maps complete search aliases to their corresponding results
		final StringMultiTrie<Result> prefixResults;
		// maps all non-prefix MAX_SUBSTRING_LENGTH-length (or less) substrings of search
		// aliases to their corresponding result; used to narrow down the search scope for substring matches
		final StringMultiTrie<Result> containingResults;

		@NonNull
		ResultCache resultCache = this.emptyCache;

		Lookup(StringMultiTrie<Result> prefixResults, StringMultiTrie<Result> containingResults) {
			this.prefixResults = prefixResults;
			this.containingResults = containingResults;
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
			static ImmutableSet<Result.ItemHolder> buildContaining(
					String term, Set<Result> possibilities,
					Set<SearchableElement> excluded
			) {
				return possibilities
					.stream()
					.filter(result -> !excluded.contains(result.searchable))
					.map(result -> result.findContainingItem(term))
					.flatMap(Optional::stream)
					.sorted()
					.collect(toImmutableSet());
			}

			final String term;
			final Node<Result> prefixNode;
			final ImmutableMap<SearchableElement, Component> prefixedItemsBySearchable;
			final ImmutableSet<Result.ItemHolder> containingItems;

			ResultCache(
					String term, Node<Result> prefixNode,
					ImmutableMap<SearchableElement, Component> prefixedItemsBySearchable,
					ImmutableSet<Result.ItemHolder> containingItems
			) {
				this.term = term;
				this.prefixNode = prefixNode;
				this.prefixedItemsBySearchable = prefixedItemsBySearchable;
				this.containingItems = containingItems;
			}

			boolean hasResults() {
				return !this.prefixNode.isEmpty() || !this.containingItems.isEmpty();
			}

			boolean hasSameResults(ResultCache other) {
				return this == other
						|| this.prefixNode == other.prefixNode
							&& this.containingItems.equals(other.containingItems);
			}

			ResultCache updated(String term) {
				if (this.term.isEmpty()) {
					return this.createFresh(term);
				} else {
					final int commonPrefixLength = getCommonPrefixLength(this.term, term);
					final int termLength = term.length();
					final int cachedTermLength = this.term.length();

					if (commonPrefixLength == 0) {
						return this.createFresh(term);
					} else if (commonPrefixLength == termLength && commonPrefixLength == cachedTermLength) {
						return this;
					} else {
						final int backSteps = cachedTermLength - commonPrefixLength;
						Node<Result> prefixNode = this.prefixNode.previous(backSteps);
						// true iff this.term is a prefix of term or vice versa
						final boolean oneTermIsPrefix;
						if (termLength > commonPrefixLength) {
							oneTermIsPrefix = backSteps == 0;

							for (int i = commonPrefixLength; i < termLength; i++) {
								prefixNode = prefixNode.next(term.charAt(i));

								if (prefixNode.isEmpty()) {
									break;
								}
							}
						} else {
							oneTermIsPrefix = true;
						}

						final ImmutableMap<SearchableElement, Component> prefixedItemsBySearchable;
						if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
							prefixedItemsBySearchable = this.prefixedItemsBySearchable;
						} else {
							prefixedItemsBySearchable = buildPrefixedItemsBySearchable(term, prefixNode);
						}

						final ImmutableSet<Result.ItemHolder> containingItems;
						if (cachedTermLength == commonPrefixLength) {
							if (termLength > MAX_SUBSTRING_LENGTH) {
								containingItems = this.narrowedContainingItemsOf(term);
							} else {
								final Set<Result> containingPossibilities = this.getContainingPossibilities(term);
								if (containingPossibilities.size() <= this.containingItems.size()) {
									containingItems = buildContaining(
										term, containingPossibilities,
										prefixedItemsBySearchable.keySet()
									);
								} else {
									containingItems = this.narrowedContainingItemsOf(term);
								}
							}
						} else {
							containingItems = this.buildContaining(term, prefixedItemsBySearchable.keySet());
						}

						return new ResultCache(term, prefixNode, prefixedItemsBySearchable, containingItems);
					}
				}
			}

			ResultCache createFresh(String term) {
				final Node<Result> prefixNode = Lookup.this.prefixResults.get(term);
				final ImmutableMap<SearchableElement, Component> prefixedItemsByElement =
						buildPrefixedItemsBySearchable(term, prefixNode);
				return new ResultCache(
					term, prefixNode,
					prefixedItemsByElement,
					this.buildContaining(term, prefixedItemsByElement.keySet())
				);
			}

			static ImmutableMap<SearchableElement, Component> buildPrefixedItemsBySearchable(
					String term, Node<Result> prefixNode
			) {
				return prefixNode
					.streamValues()
					.sorted()
					.distinct()
					.collect(toImmutableMap(
						Result::getSearchable,
						result -> result.getPrefixedItemOrThrow(term).getItem()
					));
			}

			ImmutableSet<Result.ItemHolder> narrowedContainingItemsOf(String term) {
				return this.containingItems.stream()
					.map(item -> item.getOwner().findContainingItem(term))
					.flatMap(Optional::stream)
					.collect(toImmutableSet());
			}

			ImmutableSet<Result.ItemHolder> buildContaining(String term, Set<SearchableElement> excluded) {
				return buildContaining(term, this.getContainingPossibilities(term), excluded);
			}

			Set<Result> getContainingPossibilities(String term) {
				final int termLength = term.length();
				final Set<Result> possibilities = new HashSet<>();
				for (int start = NON_PREFIX_START; start <= termLength; start++) {
					final int end = Math.min(start + MAX_SUBSTRING_LENGTH, termLength);
					Node<Result> node = Lookup.this.containingResults.getRoot();
					for (int i = start; i < end; i++) {
						node = node.next(term.charAt(i));

						if (node.isEmpty()) {
							break;
						}
					}

					node.streamValues().forEach(possibilities::add);
				}

				return possibilities;
			}
		}
	}

	private static class Result implements Comparable<Result> {
		final SearchableElement searchable;

		final ImmutableMap<String, String> aliasesByLowercase;
		final Map<String, ItemHolder> holdersByAlias;

		Result(SearchableElement searchable, ImmutableMap<String, String> aliasesByLowercase) {
			this.searchable = searchable;

			this.aliasesByLowercase = aliasesByLowercase;
			this.holdersByAlias = new HashMap<>(aliasesByLowercase.size());
		}

		@Override
		public int compareTo(@NonNull Result other) {
			return this.searchable.getSearchName().compareTo(other.searchable.getSearchName());
		}

		SearchableElement getSearchable() {
			return this.searchable;
		}

		Optional<ItemHolder> findContainingItem(String term) {
			return this.aliasesByLowercase.entrySet().stream()
				.filter(entry -> entry.getKey().contains(term))
				.findFirst()
				.map(Map.Entry::getValue)
				.map(this::getItemHolderForAlias);
		}

		/**
		 * @return the {@link ItemHolder} representing the alias prefixed with the passed {@code term}
		 *
		 * @throws IllegalArgumentException if no lowercase alias starts with the passed {@code term}
		 */
		ItemHolder getPrefixedItemOrThrow(String term) {
			return this.aliasesByLowercase.entrySet()
				.stream()
				.filter(entry -> entry.getKey().startsWith(term))
				.findFirst()
				.map(Map.Entry::getValue)
				.map(this::getItemHolderForAlias)
				.orElseThrow(() -> new IllegalArgumentException(
					"""
					No lowercase alias starts with "%s"!
					\tlowercase aliases: %s\
					""".formatted(term, this.aliasesByLowercase.keySet())
				));
		}

		ItemHolder getItemHolderForAlias(String alias) {
			return this.holdersByAlias.computeIfAbsent(alias, ItemHolder::new);
		}

		class ItemHolder implements Comparable<ItemHolder> {
			final JMenuItem item;

			ItemHolder(String matchedAlias) {
				final String searchName = Result.this.searchable.getSearchName();

				this.item = matchedAlias.equals(searchName)
						? new JMenuItem(searchName)
						: new AliasedItem(searchName, matchedAlias);
			}

			public Component getItem() {
				return this.item;
			}

			Result getOwner() {
				return Result.this;
			}

			@Override
			public int compareTo(@NonNull ItemHolder other) {
				return this.getOwner().compareTo(other.getOwner());
			}

			static class AliasedItem extends JMenuItem {
				static final int UNSET_WIDTH = -1;

				final String alias;

				int aliasWidth = UNSET_WIDTH;
				@Nullable
				Font aliasFont;

				AliasedItem(String searchName, String alias) {
					super(searchName);

					this.alias = alias;
				}

				@Override
				public void setFont(Font font) {
					super.setFont(font);

					this.aliasWidth = UNSET_WIDTH;
					this.aliasFont = null;
				}

				@Nullable
				Font getAliasFont() {
					if (this.aliasFont == null) {
						final Font font = this.getFont();
						if (font != null) {
							this.aliasFont = font.deriveFont(Font.ITALIC);
						}
					}

					return this.aliasFont;
				}

				@Override
				public Dimension getPreferredSize() {
					final Dimension size = super.getPreferredSize();

					size.width += this.getAliasWidth();

					return size;
				}

				@Override
				public void paint(Graphics graphics) {
					super.paint(graphics);

					GuiUtil.trySetRenderingHints(graphics);
					final Color color = this.getForeground();
					if (color != null) {
						graphics.setColor(color);
					}

					final Font aliasFont = this.getAliasFont();
					if (aliasFont != null) {
						graphics.setFont(aliasFont);
					}

					final Insets insets = this.getInsets();
					final int baseY = graphics.getFontMetrics().getMaxAscent() + insets.top;
					graphics.drawString(this.alias, this.getWidth() - insets.right - this.getAliasWidth(), baseY);
				}

				int getAliasWidth() {
					if (this.aliasWidth < 0) {
						this.aliasWidth = this.getFontMetrics(this.getAliasFont()).stringWidth(this.alias);
					}

					return this.aliasWidth;
				}
			}
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
					.concat(
						cache.prefixedItemsBySearchable.values().stream(),
						cache.containingItems.stream().map(Result.ItemHolder::getItem)
					)
					.collect(toImmutableList())
				);
			}
		}
	}
}

package org.quiltmc.enigma.gui.element.menu_bar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.EmptyStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie.Node;
import org.tinylog.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static javax.swing.BorderFactory.createEmptyBorder;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	private static final int MAX_INITIAL_RESULTS = 20;

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

	private static void clearSelectionAndChoose(SearchableElement searchable, MenuSelectionManager manager) {
		// clearing the path ensures:
		// - the help menu doesn't stay selected if onSearchChosen doesn't set the path
		// - the current path doesn't interfere with onSearchChosen implementations that set the
		//   path based on the current path
		manager.clearSelectedPath();
		searchable.onSearchChosen();
	}

	private static ImmutableList<MenuElement> buildPathTo(MenuElement target) {
		final List<MenuElement> pathBuilder = new LinkedList<>();
		pathBuilder.add(target);
		Component element = target.getComponent().getParent();
		while (true) {
			if (element instanceof JMenu menu) {
				pathBuilder.add(0, menu);
				element = menu.getParent();
			} else if (element instanceof JPopupMenu popup) {
				pathBuilder.add(0, popup);
				element = popup.getInvoker();
			} else {
				break;
			}
		}

		if (element instanceof JMenuBar bar) {
			pathBuilder.add(0, bar);

			return ImmutableList.copyOf(pathBuilder);
		} else {
			Logger.error(
					"""
					Failed to build path to %s!
					\tPath does not begin with menu bar: %s
					""".formatted(target, pathBuilder)
			);

			return ImmutableList.of();
		}
	}

	@Nullable
	private final Border defaultPopupBorder;

	private final PlaceheldTextField field = new PlaceheldTextField();
	private final JMenuItem noResults = new JMenuItem();

	private final HintItem viewHint = new HintItem(
			"menu.help.search.hint.view",
			Config.main().searchMenus.showViewHint
	);
	private final HintItem chooseHint = new HintItem(
			"menu.help.search.hint.choose",
			Config.main().searchMenus.showChooseHint
	);

	@Nullable
	private Lookup lookup;

	protected SearchMenusMenu(Gui gui) {
		super(gui);

		// global listener because menu/item key listeners didn't fire
		// also more reliably clears restorablePath
		Toolkit.getDefaultToolkit().addAWTEventListener(new KeyHandler(), AWTEvent.KEY_EVENT_MASK);

		this.defaultPopupBorder = this.getPopupMenu().getBorder();

		this.noResults.setEnabled(false);
		this.noResults.setVisible(false);

		this.viewHint.setVisible(false);
		this.chooseHint.setVisible(false);

		this.addPermanentChildren();

		this.field.setFont(ScaleUtil.scaleFont(this.field.getFont()));
		// Always focus field, but don't always select its text, because it loses focus when packing new search results.
		this.field.addHierarchyListener(new HierarchyListener() {
			@Nullable
			ImmutableList<MenuElement> fieldPath;

			ImmutableList<MenuElement> getFieldPath() {
				if (this.fieldPath == null) {
					this.fieldPath = buildPathTo(SearchMenusMenu.this.field);
				}

				return this.fieldPath;
			}

			@Override
			public void hierarchyChanged(HierarchyEvent e) {
				if (SearchMenusMenu.this.field.isShowing()) {
					final Window window = SwingUtilities.getWindowAncestor(SearchMenusMenu.this.field);
					if (window != null && window.getType() == Window.Type.POPUP) {
						// HACK: if PopupFactory::fitsOnScreen is false for light- and medium-weight popups, it makes a
						// heavy-weight popup instead, whose HeavyWeightWindow component is by default is not focusable.
						// It prevented this.field from focusing and receiving input.
						window.setFocusableWindowState(true);
					}

					SearchMenusMenu.this.field.requestFocus();
					MenuSelectionManager.defaultManager().setSelectedPath(this.getFieldPath().toArray(new MenuElement[0]));
				}
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

		this.retranslate();
	}

	private void updateResultItems() {
		final String searchTerm = this.field.getText();

		final Results results = this.getLookup().search(searchTerm);

		if (results instanceof Results.None) {
			this.keepOnlyPermanentChildren();

			this.viewHint.setVisible(false);
			this.chooseHint.setVisible(false);

			this.noResults.setVisible(!searchTerm.isEmpty());

			this.refreshPopup();
		} else if (results instanceof Results.Different different) {
			this.keepOnlyPermanentChildren();

			this.noResults.setVisible(different.isEmpty());
			this.viewHint.configureVisibility();
			this.chooseHint.configureVisibility();

			// truncate results because the popup lags when packing numerous items, which can cause keystroke drops
			int remainingItems = MAX_INITIAL_RESULTS;
			final UnmodifiableIterator<Result.ItemHolder.Item> prefixItr = different.prefixItems.iterator();
			while (prefixItr.hasNext() && remainingItems > 0) {
				this.add(prefixItr.next());
				remainingItems--;
			}

			final UnmodifiableIterator<Result.ItemHolder.Item> containingItr = different.containingItems.iterator();
			if (remainingItems > 0 && containingItr.hasNext()) {
				if (!different.prefixItems.isEmpty()) {
					this.add(new JPopupMenu.Separator());
				}

				while (containingItr.hasNext() && remainingItems > 0) {
					this.add(containingItr.next());
					remainingItems--;
				}
			}

			if (different.getSize() > MAX_INITIAL_RESULTS) {
				final ImmutableList.Builder<Result.ItemHolder.Item> truncatedPrefixResults = ImmutableList.builder();
				prefixItr.forEachRemaining(truncatedPrefixResults::add);

				final ImmutableList.Builder<Result.ItemHolder.Item> truncatedContainingResults = ImmutableList.builder();
				containingItr.forEachRemaining(truncatedContainingResults::add);

				this.add(this.moreButtonOf(truncatedPrefixResults.build(), truncatedContainingResults.build()));
			}

			this.refreshPopup();
		} // else Results.Same
	}

	private void refreshPopup() {
		if (this.isShowing()) {
			final JPopupMenu popupMenu = this.getPopupMenu();

			// HACK: When popups are resizing in limited space, they may remove their borders.
			// The border won't be restored when re-packing or showing, so manually restore the original border here.
			popupMenu.setBorder(this.defaultPopupBorder);
			popupMenu.pack();

			// only re-show
			// the initial showing from JMenu does the same thing and would cause an SOE if we also showed here
			if (popupMenu.isShowing()) {
				final Point newOrigin = this.getPopupMenuOrigin();
				popupMenu.show(this, newOrigin.x, newOrigin.y);
			}
		}
	}

	private void addPermanentChildren() {
		this.add(this.field);
		this.add(this.noResults);
		this.add(this.viewHint);
		this.add(this.chooseHint);
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

	private JMenuItem moreButtonOf(
			ImmutableList<Result.ItemHolder.Item> prefixItems,
			ImmutableList<Result.ItemHolder.Item> containingItems
	) {
		final var button = new JMenuItem();

		button.setText("⋯");

		button.addActionListener(e -> {
			this.remove(button);

			prefixItems.forEach(this::add);

			if (!prefixItems.isEmpty() && !containingItems.isEmpty()) {
				this.add(new JPopupMenu.Separator());
			}

			containingItems.forEach(this::add);

			// clicking the button closes the menu
			// this re-opens it (which includes re-packing and selecting the search field)
			this.doClick(0);
			// de-select and move caret to end
			this.field.setSelectionStart(Integer.MAX_VALUE);
		});

		return button;
	}

	private static final class Lookup {
		static final int NON_PREFIX_START = 1;
		static final int MAX_SUBSTRING_LENGTH = 2;

		final ResultCache emptyCache = new ResultCache(
				"", EmptyStringMultiTrie.Node.get(),
				ImmutableMap.of(), ImmutableList.of()
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
			final CompositeStringMultiTrie<Result.ItemHolder> prefixBuilder = CompositeStringMultiTrie.createHashed();
			final CompositeStringMultiTrie<Result.ItemHolder> containingBuilder =
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
						final Result result = new Result(element);

						final ImmutableMap<String, Result.ItemHolder> holders = result.createHolders();

						holders.forEach((lowercaseAlias, holder) -> {
							prefixBuilder.put(lowercaseAlias, holder);

							final int aliasLength = lowercaseAlias.length();
							for (int start = NON_PREFIX_START; start < aliasLength; start++) {
								final int end = Math.min(start + MAX_SUBSTRING_LENGTH, aliasLength);
								MutableStringMultiTrie.Node<Result.ItemHolder> node = containingBuilder.getRoot();
								for (int i = start; i < end; i++) {
									node = node.next(lowercaseAlias.charAt(i));
								}

								node.put(holder);
							}
						});
					});

			return new Lookup(prefixBuilder.view(), containingBuilder.view());
		}

		// maps complete search aliases to their corresponding items
		final StringMultiTrie<Result.ItemHolder> holdersByPrefix;
		// maps all non-prefix MAX_SUBSTRING_LENGTH-length (or less) substrings of search
		// aliases to their corresponding items; used to narrow down the search scope for substring matches
		final StringMultiTrie<Result.ItemHolder> holdersByContaining;

		@NonNull
		ResultCache resultCache = this.emptyCache;

		Lookup(StringMultiTrie<Result.ItemHolder> holdersByPrefix, StringMultiTrie<Result.ItemHolder> holdersByContaining) {
			this.holdersByPrefix = holdersByPrefix;
			this.holdersByContaining = holdersByContaining;
		}

		Results search(String term) {
			if (term.isEmpty()) {
				this.resultCache = this.emptyCache;

				return Results.None.INSTANCE;
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
			final Node<Result.ItemHolder> prefixNode;
			final ImmutableMap<SearchableElement, Result.ItemHolder.Item> prefixedItemsBySearchable;
			final ImmutableList<Result.ItemHolder.Item> containingItems;

			ResultCache(
					String term, Node<Result.ItemHolder> prefixNode,
					ImmutableMap<SearchableElement, Result.ItemHolder.Item> prefixedItemsBySearchable,
					ImmutableList<Result.ItemHolder.Item> containingItems
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
						Node<Result.ItemHolder> prefixNode = this.prefixNode.previous(backSteps);
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

						final ImmutableMap<SearchableElement, Result.ItemHolder.Item> prefixedItemsBySearchable;
						if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
							prefixedItemsBySearchable = this.prefixedItemsBySearchable;
						} else {
							prefixedItemsBySearchable = buildPrefixedItemsBySearchable(prefixNode);
						}

						final ImmutableList<Result.ItemHolder.Item> containingItems;
						if (cachedTermLength == commonPrefixLength && termLength > MAX_SUBSTRING_LENGTH) {
							containingItems = this.narrowedContainingItemsOf(term);
						} else {
							containingItems = this.buildContaining(term, prefixedItemsBySearchable.keySet());
						}

						return new ResultCache(term, prefixNode, prefixedItemsBySearchable, containingItems);
					}
				}
			}

			ResultCache createFresh(String term) {
				final Node<Result.ItemHolder> prefixNode = Lookup.this.holdersByPrefix.get(term);
				final ImmutableMap<SearchableElement, Result.ItemHolder.Item> prefixedItemsByElement =
						buildPrefixedItemsBySearchable(prefixNode);
				return new ResultCache(
					term, prefixNode,
					prefixedItemsByElement,
					this.buildContaining(term, prefixedItemsByElement.keySet())
				);
			}

			static ImmutableMap<SearchableElement, Result.ItemHolder.Item> buildPrefixedItemsBySearchable(
					Node<Result.ItemHolder> prefixNode
			) {
				return prefixNode
					.streamValues()
					.sorted()
					.collect(toImmutableMap(
						Result.ItemHolder::getSearchable,
						Result.ItemHolder::getItem,
						// if aliases share a prefix, try keeping non-aliased item
						(left, right) -> right.isSearchNamed() && !left.isSearchNamed() ? right : left
					));
			}

			ImmutableList<Result.ItemHolder.Item> narrowedContainingItemsOf(String term) {
				return this.containingItems.stream()
					.filter(item -> item.getHolder().lowercaseAlias.contains(term))
					.collect(toImmutableList());
			}

			ImmutableList<Result.ItemHolder.Item> buildContaining(String term, Set<SearchableElement> excluded) {
				final int termLength = term.length();
				final boolean longTerm = termLength > MAX_SUBSTRING_LENGTH;

				final Set<Result.ItemHolder> possibilities = new HashSet<>();
				final int substringLength = longTerm ? MAX_SUBSTRING_LENGTH : termLength;
				final int lastSubstringStart = termLength - substringLength;
				for (int start = 0; start <= lastSubstringStart; start++) {
					final int end = start + substringLength;
					Node<Result.ItemHolder> node = Lookup.this.holdersByContaining.getRoot();
					for (int i = start; i < end; i++) {
						node = node.next(term.charAt(i));

						if (node.isEmpty()) {
							break;
						}
					}

					node.streamValues().forEach(possibilities::add);
				}

				Stream<Result.ItemHolder> stream = possibilities
						.stream()
						.filter(holder -> !excluded.contains(holder.getSearchable()));

				if (longTerm) {
					stream = stream.filter(holder -> holder.lowercaseAlias.contains(term));
				}

				return stream
					.sorted()
					.map(Result.ItemHolder::getItem)
					.collect(toImmutableList());
			}
		}
	}

	/**
	 * A wrapper for a {@link SearchableElement}.
	 *
	 * <p> Its only purpose is to link its (non-static) inner classes to the same {@link SearchableElement}.
	 */
	private record Result(SearchableElement searchable) {
		ImmutableMap<String, ItemHolder> createHolders() {
			final String searchName = this.searchable.getSearchName();
			return this.searchable.streamSearchAliases()
				.filter(alias -> !alias.isEmpty())
				.map(alias -> Map.entry(alias.toLowerCase(), alias))
				.collect(toImmutableMap(
					Map.Entry::getKey,
					entry -> new ItemHolder(searchName, entry.getKey(), entry.getValue()),
					// ignore case-insensitive duplicate aliases
					(left, right) -> left
				));
		}

		/**
		 * A holder for a {@linkplain #getItem() lazily-created} {@link Item}.
		 *
		 * <p> Contains the information needed to determine whether its item should be included in results.
		 */
		class ItemHolder implements Comparable<ItemHolder> {
			private final String searchName;
			private final String alias;
			final String lowercaseAlias;

			@Nullable
			Item item;

			ItemHolder(String searchName, String lowercaseAlias, String alias) {
				this.searchName = searchName;
				this.lowercaseAlias = lowercaseAlias;
				this.alias = alias;
			}

			Item getItem() {
				if (this.item == null) {
					this.item = this.alias.equals(this.searchName)
							? new Item(this.searchName)
							: new AliasedItem(this.searchName, this.alias);
				}

				return this.item;
			}

			SearchableElement getSearchable() {
				return Result.this.searchable();
			}

			Result getResult() {
				return Result.this;
			}

			@Override
			public int compareTo(@NonNull ItemHolder other) {
				return this.getSearchable().getSearchName().compareTo(other.getSearchable().getSearchName());
			}

			@Override
			public int hashCode() {
				return this.getSearchable().hashCode();
			}

			@Override
			public boolean equals(Object o) {
				return o instanceof ItemHolder other && this.getSearchable() == other.getSearchable();
			}

			class Item extends JMenuItem {
				private final ImmutableList<MenuElement> path;

				Item(String searchName) {
					super(searchName);

					this.addActionListener(e -> {
						clearSelectionAndChoose(Result.this.searchable, MenuSelectionManager.defaultManager());
					});

					this.path = buildPathTo(this.getSearchable());

					if (!this.path.isEmpty()) {
						final String pathText = this.path.stream()
								.flatMap(element -> {
									if (element instanceof SearchableElement searchableElement) {
										return Stream.of(searchableElement.getSearchName());
									} else if (element.getComponent() instanceof JMenuItem menuItem) {
										return Stream.of(menuItem.getText());
									} else {
										// JPopupMenus' names come from their parent JMenus; skip them
										// JMenuBar has no name
										if (element instanceof JPopupMenu || element instanceof JMenuBar) {
											return Stream.empty();
										} else {
											Logger.error(
													"Cannot determine name of menu element in path to %s: %s"
														.formatted(searchName, element)
											);

											return Stream.of("???");
										}
									}
								})
								.collect(Collectors.joining(" > "));

						this.setToolTipText(pathText);
					}
				}

				boolean isSearchNamed() {
					return true;
				}

				ItemHolder getHolder() {
					return ItemHolder.this;
				}

				void selectSearchable() {
					if (!this.path.isEmpty()) {
						MenuSelectionManager.defaultManager().setSelectedPath(this.path.toArray(new MenuElement[0]));
					}
				}

				private SearchableElement getSearchable() {
					return this.getHolder().getResult().searchable;
				}
			}

			class AliasedItem extends Item {
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
				boolean isSearchNamed() {
					return false;
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

		record Different(
				ImmutableList<Result.ItemHolder.Item> prefixItems,
				ImmutableList<Result.ItemHolder.Item> containingItems
		) implements Results {
			static Different of(Lookup.ResultCache cache) {
				return new Different(
						cache.prefixedItemsBySearchable.values().stream().distinct().collect(toImmutableList()),
						cache.containingItems.stream().distinct().collect(toImmutableList())
				);
			}

			boolean isEmpty() {
				return this.prefixItems.isEmpty() && this.containingItems.isEmpty();
			}

			int getSize() {
				return this.prefixItems.size() + this.containingItems.size();
			}
		}
	}

	private class KeyHandler implements AWTEventListener {
		static final int PREVIEW_MODIFIER_MASK = InputEvent.SHIFT_DOWN_MASK;
		static final int PREVIEW_MODIFIER_KEY = KeyEvent.VK_SHIFT;

		@Nullable
		static <T> T getLastOrNull(T[] array) {
			if (array.length > 0) {
				return array[array.length - 1];
			} else {
				return null;
			}
		}

		@Nullable
		RestorablePath restorablePath;

		@Override
		public void eventDispatched(AWTEvent e) {
			if (e instanceof KeyEvent keyEvent) {
				if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
					final int keyCode = keyEvent.getKeyCode();
					if (keyCode == PREVIEW_MODIFIER_KEY && keyEvent.getModifiersEx() == PREVIEW_MODIFIER_MASK) {
						final MenuElement[] selectedPath = MenuSelectionManager.defaultManager().getSelectedPath();

						final MenuElement selected = getLastOrNull(selectedPath);
						if (selected != null) {
							if (selected instanceof Result.ItemHolder.Item item) {
								SearchMenusMenu.this.viewHint.dismiss();

								this.restorablePath = new RestorablePath(item.getSearchable(), selectedPath);

								item.selectSearchable();

								return;
							} else if (this.restorablePath != null && this.restorablePath.searched == selected) {
								return;
							}
						}
					} else if (keyCode == KeyEvent.VK_ENTER) {
						final int modifiers = keyEvent.getModifiersEx();
						if (modifiers == 0) {
							final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
							if (getLastOrNull(manager.getSelectedPath()) instanceof Result.ItemHolder.Item item) {
								this.execute(item.getSearchable(), manager);
							}
						} else if (modifiers == PREVIEW_MODIFIER_MASK) {
							if (this.restorablePath != null) {
								final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
								if (this.restorablePath.searched == getLastOrNull(manager.getSelectedPath())) {
									this.execute(this.restorablePath.searched, manager);
								}
							}
						}
					}

					this.restorablePath = null;
				} else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
					if (this.restorablePath != null) {
						if (keyEvent.getKeyCode() == PREVIEW_MODIFIER_KEY) {
							if (keyEvent.getModifiersEx() == 0) {
								MenuSelectionManager.defaultManager().setSelectedPath(this.restorablePath.helpPath);
							}
						} else {
							this.restorablePath = null;
						}
					}
				}
			}
		}

		void execute(SearchableElement searchable, MenuSelectionManager manager) {
			SearchMenusMenu.this.chooseHint.dismiss();
			clearSelectionAndChoose(searchable, manager);
		}

		record RestorablePath(SearchableElement searched, MenuElement[] helpPath) { }
	}

	// not a MenuElement so it can't be selected
	private class HintItem extends JPanel implements Retranslatable {
		final String translationKey;
		final TrackedValue<Boolean> config;

		final JLabel infoIndicator = new JLabel("ⓘ");
		final JLabel hint = new JLabel();
		final JButton dismissButton = new JButton("⊗");

		HintItem(String translationKey, TrackedValue<Boolean> config) {
			this.translationKey = translationKey;
			this.config = config;

			this.setBorder(createEmptyBorder(0, 2, 0, 0));

			this.setLayout(new GridBagLayout());

			this.add(this.infoIndicator);

			final var spacer = Box.createHorizontalBox();
			spacer.setPreferredSize(new Dimension(3, 1));
			this.add(spacer);

			final Font oldHintFont = this.hint.getFont();
			this.hint.setFont(oldHintFont.deriveFont(Font.ITALIC, oldHintFont.getSize2D() * 0.85f));
			this.add(this.hint, GridBagConstraintsBuilder.create()
					.weightX(1)
					.fill(GridBagConstraints.HORIZONTAL)
					.build()
			);

			this.dismissButton.setBorderPainted(false);
			this.dismissButton.setBackground(new Color(0, true));
			this.dismissButton.setMargin(new Insets(0, 0, 0, 0));
			final Font oldDismissFont = this.dismissButton.getFont();
			this.dismissButton.setFont(oldDismissFont.deriveFont(oldDismissFont.getSize2D() * 1.5f));
			this.dismissButton.addActionListener(e -> this.dismiss());
			this.add(this.dismissButton);

			this.retranslate();
		}

		void dismiss() {
			this.config.setValue(false);
			this.setVisible(false);
			SearchMenusMenu.this.refreshPopup();
		}

		void configureVisibility() {
			this.setVisible(this.config.value());
		}

		@Override
		public void retranslate() {
			this.hint.setText(I18n.translate(this.translationKey));
		}
	}
}

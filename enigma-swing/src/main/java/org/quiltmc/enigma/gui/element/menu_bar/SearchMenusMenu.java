package org.quiltmc.enigma.gui.element.menu_bar;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Lazy;
import org.quiltmc.enigma.util.StringLookup;
import org.tinylog.Logger;

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
import javax.swing.event.ChangeListener;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.quiltmc.enigma.gui.util.GuiUtil.EMPTY_MENU_ELEMENTS;
import static org.quiltmc.enigma.util.Utils.getLastOrNull;
import static javax.swing.BorderFactory.createEmptyBorder;

public class SearchMenusMenu extends AbstractEnigmaMenu {
	/**
	 * @return a stream of the passed {@code root} element and all of its sub-elements,
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

	private final PlaceheldMenuTextField field = new PlaceheldMenuTextField();
	private final JMenuItem noResults = new JMenuItem();

	private final HintItem viewHint = new HintItem(
			"menu.help.search.hint.view",
			Config.main().searchMenus.showViewHint
	);
	private final HintItem chooseHint = new HintItem(
			"menu.help.search.hint.choose",
			Config.main().searchMenus.showChooseHint
	);

	private final Lazy.Clearable<StringLookup<Result>> lookup = Lazy.clearableOf(() -> StringLookup.of(
			2, Result.COMPARATOR, this.gui
				.getMenuBar()
				.streamMenus()
				.flatMap(SearchMenusMenu::streamElementTree)
				.flatMap(element -> element instanceof SearchableElement searchable
					? Result.stream(searchable)
					: Stream.empty()
				)
				.toList()
	));

	private final Lazy<ImmutableList<MenuElement>> fieldPath = Lazy.of(() -> buildPathTo(this.field));

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

		MenuSelectionManager.defaultManager().addChangeListener(e -> {
			if (this.field.isShowing()) {
				final var manager = (MenuSelectionManager) e.getSource();
				if (getLastOrNull(manager.getSelectedPath()) == this.getPopupMenu()) {
					// select here instead of in the hierarchy listener below because:
					// 1. the manager doesn't report the final path in a hierarchy listener
					// 2. selecting from the hierarchy listener caused bugs when restoring after showing a result:
					//    - this.field and the restored item both appeared selected, but arrow keys couldn't select
					//    - the final selected path got an increasing number of duplicates of this.field;
					//      none should have been in the path
					this.selectField(manager);
				}
			}
		});

		// Always focus field, but don't always select its text, because it loses focus when packing new search results.
		this.field.addHierarchyListener(e -> {
			if (this.field.isShowing()) {
				final Window window = SwingUtilities.getWindowAncestor(this.field);
				if (window != null && window.getType() == Window.Type.POPUP) {
					// HACK: if PopupFactory::fitsOnScreen is false for light- and medium-weight popups, it makes a
					// heavy-weight popup instead, whose HeavyWeightWindow component is by default is not focusable.
					// It prevented this.field from focusing and receiving input.
					window.setFocusableWindowState(true);
				}

				this.field.requestFocus();
			}
		});

		// select field on content change so shift capitalizes instead of viewing selection
		this.field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				SearchMenusMenu.this.selectField(MenuSelectionManager.defaultManager());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				SearchMenusMenu.this.selectField(MenuSelectionManager.defaultManager());
			}

			@Override
			public void changedUpdate(DocumentEvent e) { }
		});

		this.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			// menu-select field on caret change so shift capitalizes instead of viewing selection
			final ChangeListener selectFieldOnCaretChange =
					e -> SearchMenusMenu.this.selectField(MenuSelectionManager.defaultManager());

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SearchMenusMenu.this.field.selectAll();
				// Only listen for text selection after initial text selection (see popupMenuWillBecomeInvisible).
				SearchMenusMenu.this.field.getCaret().addChangeListener(this.selectFieldOnCaretChange);

				SearchMenusMenu.this.updateResultItems();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// Don't listen for text selection before initial text selection so field doesn't get menu-selected
				// when releasing shift to return from viewing.
				SearchMenusMenu.this.field.getCaret().removeChangeListener(this.selectFieldOnCaretChange);
			}

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

	private void selectField(MenuSelectionManager manager) {
		manager.setSelectedPath(this.fieldPath.get().toArray(EMPTY_MENU_ELEMENTS));
	}

	private void updateResultItems() {
		final String term = this.field.getText();

		this.lookup.get().lookUpDifferent(term).ifPresentOrElse(
				results -> {
					if (results.areEmpty()) {
						this.keepOnlyPermanentChildren();

						this.noResults.setVisible(!term.isEmpty());
						this.viewHint.setVisible(false);
						this.chooseHint.setVisible(false);

						this.refreshPopup();
					} else {
						this.keepOnlyPermanentChildren();

						this.noResults.setVisible(false);
						this.viewHint.configureVisibility();
						this.chooseHint.configureVisibility();

						results.prefixed().stream().map(Result::getItem).forEach(this::add);

						if (!results.containing().isEmpty()) {
							if (!results.prefixed().isEmpty()) {
								this.add(new JPopupMenu.Separator());
							}

							results.containing().stream().map(Result::getItem).forEach(this::add);
						}

						this.refreshPopup();
					}
				},
				() -> {
					// in case term went directly from empty to a term with no results (usually via pasting)
					// or vice versa, update noResults visibility so it only shows if there's a term
					final boolean noTerm = term.isEmpty();
					if (noTerm == this.noResults.isVisible()) {
						this.noResults.setVisible(!noTerm);
						this.refreshPopup();
					}
				}
		);
	}

	private void refreshPopup() {
		if (this.isShowing()) {
			final JPopupMenu popupMenu = this.getPopupMenu();

			final int oldHeight = popupMenu.getHeight();
			// HACK: When popups are resizing in limited space, they may remove their borders.
			// The border won't be restored when re-packing or showing, so manually restore the original border here.
			popupMenu.setBorder(this.defaultPopupBorder);
			popupMenu.pack();

			// HACK: re-show if shrinking to move the popup back down in case it had to be shifted up to fit items
			// re-showing can also result in dropped keystrokes; do so as infrequently as possible
			// note: the initial showing from JMenu would cause an SOE if we also showed here for the initial showing
			if (popupMenu.getHeight() < oldHeight && popupMenu.isShowing()) {
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

	public void clearLookup() {
		this.lookup.clear();
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

	/**
	 * A result that {@linkplain #getItem() lazily-creates} an {@link Item}.
	 *
	 * <p> Contains the information needed to determine whether its item should be included in results.
	 */
	private static class Result implements StringLookup.Result {
		static final Comparator<Result> COMPARATOR = Comparator
				.<Result, String>comparing(result -> result.searchable.getSearchName())
				// put aliased results last so non-aliased results are kept in the case of duplicates
				.thenComparing(Result::isAliased);

		static Stream<Result> stream(SearchableElement searchable) {
			final String searchName = searchable.getSearchName();
			return searchable.streamSearchAliases()
				.filter(alias -> !alias.isEmpty())
				.map(alias -> new Result(searchable, searchName, alias.toLowerCase(), alias));
		}

		final SearchableElement searchable;
		final String searchName;
		final String alias;
		final String lowercaseAlias;

		final Lazy<Boolean> aliased;
		final Lazy<Item> item;

		Result(SearchableElement searchable, String searchName, String lowercaseAlias, String alias) {
			this.searchable = searchable;
			this.searchName = searchName;
			this.lowercaseAlias = lowercaseAlias;
			this.alias = alias;

			this.aliased = Lazy.of(() -> !this.alias.equals(this.searchName));
			this.item = Lazy.of(() -> this.aliased.get()
				? new AliasedItem(this.searchName, this.alias)
				: new Item(this.searchName)
			);
		}

		boolean isAliased() {
			return this.aliased.get();
		}

		Item getItem() {
			return this.item.get();
		}

		@Override
		public String lookupString() {
			return this.lowercaseAlias;
		}

		@Override
		public SearchableElement target() {
			return this.searchable;
		}

		class Item extends JMenuItem {
			final ImmutableList<MenuElement> searchablePath;

			Item(String searchName) {
				super(searchName);

				this.addActionListener(e -> {
					clearSelectionAndChoose(Result.this.searchable, MenuSelectionManager.defaultManager());
				});

				this.searchablePath = buildPathTo(this.getSearchable());

				if (!this.searchablePath.isEmpty()) {
					final String pathText = this.searchablePath.stream()
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

			void selectSearchable(MenuSelectionManager manager) {
				if (!this.searchablePath.isEmpty()) {
					manager.setSelectedPath(this.searchablePath.toArray(EMPTY_MENU_ELEMENTS));
				}
			}

			SearchableElement getSearchable() {
				return Result.this.searchable;
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

				final Graphics disposableGraphics = graphics.create();
				GuiUtil.trySetRenderingHints(disposableGraphics);
				final Color color = this.getForeground();
				if (color != null) {
					disposableGraphics.setColor(color);
				}

				final Font aliasFont = this.getAliasFont();
				if (aliasFont != null) {
					disposableGraphics.setFont(aliasFont);
				}

				final Insets insets = this.getInsets();
				final int baseY = disposableGraphics.getFontMetrics().getMaxAscent() + insets.top;
				disposableGraphics.drawString(this.alias, this.getWidth() - insets.right - this.getAliasWidth(), baseY);

				disposableGraphics.dispose();
			}

			int getAliasWidth() {
				if (this.aliasWidth < 0) {
					this.aliasWidth = this.getFontMetrics(this.getAliasFont()).stringWidth(this.alias);
				}

				return this.aliasWidth;
			}
		}
	}

	private class KeyHandler implements AWTEventListener {
		static final int PREVIEW_MODIFIER_MASK = InputEvent.SHIFT_DOWN_MASK;
		static final int PREVIEW_MODIFIER_KEY = KeyEvent.VK_SHIFT;

		@Nullable
		RestorablePath restorablePath;

		@Override
		public void eventDispatched(AWTEvent e) {
			if (e instanceof KeyEvent keyEvent) {
				if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
					final int keyCode = keyEvent.getKeyCode();
					if (keyCode == PREVIEW_MODIFIER_KEY && keyEvent.getModifiersEx() == PREVIEW_MODIFIER_MASK) {
						final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
						final MenuElement[] selectedPath = manager.getSelectedPath();

						final MenuElement selected = getLastOrNull(selectedPath);
						if (selected != null) {
							if (selected instanceof Result.Item item) {
								SearchMenusMenu.this.viewHint.dismiss();

								this.restorablePath = new RestorablePath(item.getSearchable(), selectedPath);

								item.selectSearchable(manager);

								return;
							} else if (this.restorablePath != null && this.restorablePath.searched == selected) {
								return;
							}
						}
					} else if (keyCode == KeyEvent.VK_ENTER) {
						final int modifiers = keyEvent.getModifiersEx();
						if (modifiers == 0) {
							final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
							if (getLastOrNull(manager.getSelectedPath()) instanceof Result.Item item) {
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
								this.restorablePath = null;
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
		static final int PAD = ScaleUtil.scale(3);

		final String translationKey;
		final TrackedValue<Boolean> config;

		final JLabel infoIndicator = new JLabel("ⓘ");
		final JLabel hint = new JLabel();
		final JButton dismissButton = new JButton("⊗");

		HintItem(String translationKey, TrackedValue<Boolean> config) {
			this.translationKey = translationKey;
			this.config = config;

			this.setBorder(createEmptyBorder(0, PAD, 0, PAD));

			this.setLayout(new GridBagLayout());

			this.infoIndicator.setBorder(createEmptyBorder(0, 0, 0, PAD));
			this.add(this.infoIndicator);

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
			this.dismissButton.setFont(oldDismissFont.deriveFont(oldDismissFont.getSize2D() * 1.3f));
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
			this.dismissButton.setToolTipText(I18n.translate("prompt.dismiss"));
		}
	}
}

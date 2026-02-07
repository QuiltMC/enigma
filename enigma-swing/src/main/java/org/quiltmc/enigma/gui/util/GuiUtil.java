package org.quiltmc.enigma.gui.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleEnigmaMenu;
import org.quiltmc.enigma.impl.plugin.RecordIndexingService;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Os;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GuiUtil {
	public static final Color TRANSPARENT = new Color(0, true);

	private GuiUtil() {
		throw new UnsupportedOperationException();
	}

	public static final Icon CLASS_ICON = loadIcon("class");
	public static final Icon INTERFACE_ICON = loadIcon("interface");
	public static final Icon ENUM_ICON = loadIcon("enum");
	public static final Icon ANNOTATION_ICON = loadIcon("annotation");
	public static final Icon RECORD_ICON = loadIcon("record");
	public static final Icon METHOD_ICON = loadIcon("method");
	public static final Icon FIELD_ICON = loadIcon("field");
	public static final Icon CONSTRUCTOR_ICON = loadIcon("constructor");

	// icons sourced from https://github.com/primer/octicons
	public static final Icon OBFUSCATED_ICON = loadIcon("obfuscated");
	public static final Icon PARTIALLY_DEOBFUSCATED_ICON = loadIcon("partially_deobfuscated");
	public static final Icon DEOBFUSCATED_ICON = loadIcon("deobfuscated");
	public static final Icon PENDING_STATUS_ICON = loadIcon("pending_status");

	public static final Icon CHEVRON_UP_BLACK = loadIcon("chevron-up-black");
	public static final Icon CHEVRON_DOWN_BLACK = loadIcon("chevron-down-black");
	public static final Icon CHEVRON_UP_WHITE = loadIcon("chevron-up-white");
	public static final Icon CHEVRON_DOWN_WHITE = loadIcon("chevron-down-white");

	public static final MenuElement[] EMPTY_MENU_ELEMENTS = new MenuElement[0];

	private static final String DESKTOP_FONT_HINTS_KEY = "awt.font.desktophints";

	@Nullable
	private static Map<?, ?> desktopFontHints = Toolkit.getDefaultToolkit().getDesktopProperty(DESKTOP_FONT_HINTS_KEY)
			instanceof Map<?, ?> map ? map : null;

	static {
		Toolkit.getDefaultToolkit().addPropertyChangeListener(DESKTOP_FONT_HINTS_KEY, e -> {
			desktopFontHints = e.getNewValue() instanceof Map<?, ?> map ? map : null;
		});
	}

	public static void trySetRenderingHints(Graphics graphics) {
		if (graphics instanceof Graphics2D graphics2D) {
			if (desktopFontHints == null) {
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			} else {
				graphics2D.setRenderingHints(desktopFontHints);
			}
		}
	}

	public static void openUrl(String url) {
		try {
			if (Objects.requireNonNull(Os.getOs()) == Os.LINUX) {
				new ProcessBuilder("/usr/bin/env", "xdg-open", url).start();
			} else {
				if (Desktop.isDesktopSupported()) {
					Desktop desktop = Desktop.getDesktop();
					desktop.browse(new URI(url));
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}

	/**
	 * Puts the provided {@code text} in the system clipboard.
	 */
	public static void copyToClipboard(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	public static String getClipboard() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
			return "";
		}
	}

	public static void showPopup(JComponent component, String text, int x, int y) {
		// from https://stackoverflow.com/questions/39955015/java-swing-show-tooltip-as-a-message-dialog
		JToolTip tooltip = new JToolTip();
		tooltip.setTipText(text);
		Popup p = PopupFactory.getSharedInstance().getPopup(component, tooltip, x + 10, y);
		p.show();
		Timer t = new Timer(1000, e -> p.hide());
		t.setRepeats(false);
		t.start();
	}

	public static JLabel createLink(String text, Runnable action) {
		JLabel link = new JLabel(text);
		link.setForeground(Color.BLUE.darker());
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		@SuppressWarnings("unchecked")
		Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) link.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		link.setFont(link.getFont().deriveFont(attributes));
		link.addMouseListener(onMousePress(e -> action.run()));
		return link;
	}

	public static Icon loadIcon(String name) {
		String path = "icons/" + name + ".svg";

		// Do an eager check for a missing icon since FlatSVGIcon does it later at render time
		if (GuiUtil.class.getResource('/' + path) == null) {
			throw new NoSuchElementException("Missing icon: '" + name + "' at " + path);
		}

		// Note: the width and height are scaled automatically because the FlatLaf UI scale
		// is set in LookAndFeel.setGlobalLAF()
		return new FlatSVGIcon(path, 16, 16, GuiUtil.class.getClassLoader());
	}

	public static Icon getClassIcon(Gui gui, ClassEntry entry) {
		EntryIndex entryIndex = gui.getController().getProject().getJarIndex().getIndex(EntryIndex.class);
		AccessFlags access = entryIndex.getClassAccess(entry);

		if (access != null) {
			if (access.isAnnotation()) {
				return ANNOTATION_ICON;
			} else if (access.isInterface()) {
				return INTERFACE_ICON;
			} else if (access.isEnum()) {
				return ENUM_ICON;
			} else if (entryIndex.getDefinition(entry).isRecord()) {
				return RECORD_ICON;
			}
		}

		return CLASS_ICON;
	}

	public static Icon getFolderIcon(DefaultTreeCellRenderer renderer, JTree tree, DefaultMutableTreeNode node) {
		boolean expanded = tree.isExpanded(new TreePath(node.getPath()));
		return expanded ? renderer.getOpenIcon() : renderer.getClosedIcon();
	}

	public static Icon getDeobfuscationIcon(ProjectStatsResult stats, String packageName) {
		if (stats != null && stats.getPackageStats(packageName) != null) {
			double percentage = stats.getPackageStats(packageName).getPercentage();

			if (percentage == 100d) {
				return DEOBFUSCATED_ICON;
			} else if (percentage > 0) {
				return PARTIALLY_DEOBFUSCATED_ICON;
			}
		}

		return OBFUSCATED_ICON;
	}

	public static Icon getDeobfuscationIcon(ProjectStatsResult stats, ClassEntry obfEntry) {
		if (stats != null && stats.getStats().get(obfEntry) != null) {
			double percentage = stats.getStats().get(obfEntry).getPercentage();

			if (percentage == 100d) {
				return DEOBFUSCATED_ICON;
			} else if (percentage > 0) {
				return PARTIALLY_DEOBFUSCATED_ICON;
			}
		}

		return OBFUSCATED_ICON;
	}

	public static Icon getMethodIcon(MethodEntry entry) {
		if (entry.isConstructor()) {
			return CONSTRUCTOR_ICON;
		}

		return METHOD_ICON;
	}

	public static Icon getCloseIcon() {
		return UIManager.getIcon("InternalFrame.closeIcon");
	}

	public static TreePath getPathToRoot(TreeNode node) {
		List<TreeNode> nodes = new ArrayList<>();
		TreeNode n = node;

		do {
			nodes.add(n);
			n = n.getParent();
		} while (n != null);

		Collections.reverse(nodes);
		return new TreePath(nodes.toArray());
	}

	public static MouseListener onMouseClick(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				op.accept(e);
			}
		};
	}

	public static MouseListener onMousePress(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				op.accept(e);
			}
		};
	}

	public static KeyListener onKeyPress(Consumer<KeyEvent> op) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				op.accept(e);
			}
		};
	}

	public static WindowListener onWindowClose(Consumer<WindowEvent> op) {
		return new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				op.accept(e);
			}
		};
	}

	/**
	 * A hack method to set up the Swing glass pane, because of course it doesn't actually display anything
	 * unless you do this. Thanks Swing!
	 */
	public static void setUpGlassPane(JPanel glass) {
		glass.setOpaque(false);
		glass.setLayout(null);
		glass.revalidate();
	}

	public static Icon getUpChevron() {
		return ThemeUtil.isDarkLaf() ? CHEVRON_UP_WHITE : CHEVRON_UP_BLACK;
	}

	public static Icon getDownChevron() {
		return ThemeUtil.isDarkLaf() ? CHEVRON_DOWN_WHITE : CHEVRON_DOWN_BLACK;
	}

	public static void putKeyBindAction(KeyBind keyBind, JComponent component, ActionListener listener) {
		putKeyBindAction(keyBind, component, FocusCondition.WHEN_IN_FOCUSED_WINDOW, listener);
	}

	public static void putKeyBindAction(
			KeyBind keyBind, JComponent component, FocusCondition condition, ActionListener listener
	) {
		putKeyBindAction(keyBind, component, condition, new SimpleAction(listener));
	}

	public static void putKeyBindAction(
			KeyBind keyBind, JComponent component, FocusCondition condition, Action action
	) {
		final InputMap inputMap = component.getInputMap(condition.value);

		if (inputMap != null) {
			final String actionKey = keyBind.name();

			final KeyStroke[] keys = inputMap.keys();
			if (keys != null) {
				for (final KeyStroke key : keys) {
					final Object value = inputMap.get(key);
					if (actionKey.equals(value)) {
						// remove previous bindings to action
						inputMap.remove(key);
					}
				}
			}

			keyBind.combinations().stream().map(combo -> combo.toKeyStroke(0))
					.forEach(key -> inputMap.put(key, actionKey));

			final ActionMap actionMap = component.getActionMap();
			if (actionMap != null) {
				actionMap.remove(actionKey);
				actionMap.put(actionKey, action);
			}
		}
	}

	/**
	 * @see #consumeMousePositionIn(Component, BiConsumer, Consumer)
	 */
	public static void consumeMousePositionIn(Component component, BiConsumer<Point, Point> inAction) {
		consumeMousePositionIn(component, inAction, pos -> { });
	}

	/**
	 * @see #consumeMousePositionIn(Component, BiConsumer, Consumer)
	 */
	public static void consumeMousePositionOut(Component component, Consumer<Point> outAction) {
		consumeMousePositionIn(component, (absolut, relative) -> { }, outAction);
	}

	/**
	 * If the passed {@code component} {@link Component#contains(Point) contains} the mouse, passes the absolute mouse
	 * position and its position relative to the passed {@code component} to the passed {@code inAction}.<br>
	 * Otherwise, passes the absolute mouse position to the passed {@code outAction}.
	 *
	 * @param component the component which may contain the mouse pointer
	 * @param inAction  the action to run if the mouse is inside the passed {@code component};
	 *                  receives the mouse's absolute position and its position relative to the component
	 * @param outAction the action to run if the mouse is outside the passed {@code component};
	 *                  receives the mouse's absolute position
	 */
	public static void consumeMousePositionIn(
			Component component, BiConsumer<Point, Point> inAction, Consumer<Point> outAction
	) {
		final Point absolutePos = MouseInfo.getPointerInfo().getLocation();
		if (component.isShowing()) {
			final Point relativePos = getRelativePos(component, absolutePos);

			if (component.contains(relativePos)) {
				inAction.accept(absolutePos, relativePos);
				return;
			}
		}

		outAction.accept(absolutePos);
	}

	public static Point getRelativePos(Component component, Point absolutePos) {
		return getRelativePos(component, absolutePos.x, absolutePos.y);
	}

	public static Point getRelativePos(Component component, int absoluteX, int absoluteY) {
		final Point componentPos = component.getLocationOnScreen();
		componentPos.setLocation(-componentPos.x, -componentPos.y);
		componentPos.translate(absoluteX, absoluteY);

		return componentPos;
	}

	public static Point getAbsolutePos(Component component, int relativeX, int relativeY) {
		final Point componentPos = component.getLocationOnScreen();
		componentPos.translate(relativeX, relativeY);

		return componentPos;
	}

	public static Optional<RecordIndexingService> getRecordIndexingService(Gui gui) {
		return gui.getController()
				.getProject()
				.getEnigma()
				.getService(JarIndexerService.TYPE, RecordIndexingService.ID)
				.map(service -> (RecordIndexingService) service);
	}

	/**
	 * Creates a {@link JCheckBoxMenuItem} that is kept in sync with the passed {@code config}.
	 *
	 * @see #syncStateWithConfig(JCheckBoxMenuItem, TrackedValue)
	 */
	public static JCheckBoxMenuItem createSyncedMenuCheckBox(TrackedValue<Boolean> config) {
		final var box = new JCheckBoxMenuItem();
		syncStateWithConfig(box, config);

		return box;
	}

	/**
	 * Creates a {@link JCheckBox} that is kept in sync with the passed {@code config}.
	 *
	 * @see #syncStateWithConfig(JCheckBox, TrackedValue)
	 */
	public static JCheckBox createSyncedCheckBox(TrackedValue<Boolean> config) {
		final var box = new JCheckBox();
		syncStateWithConfig(box, config);

		return box;
	}

	/**
	 * Adds listeners to the passed {@code box} and {@code config} that keep the
	 * {@link JCheckBoxMenuItem#getState() state} of the {@code box} and the
	 * {@link TrackedValue#value() value} of the {@code config} in sync.
	 *
	 * @see #createSyncedMenuCheckBox(TrackedValue)
	 */
	public static void syncStateWithConfig(JCheckBoxMenuItem box, TrackedValue<Boolean> config) {
		syncStateWithConfigImpl(box, box::setState, box::getState, config);
	}

	/**
	 * Adds listeners to the passed {@code box} and {@code config} that keep the
	 * {@linkplain JCheckBox#isSelected() selected state} of the {@code box} and the
	 * {@link TrackedValue#value() value} of the {@code config} in sync.
	 *
	 * @see #createSyncedCheckBox(TrackedValue)
	 */
	public static void syncStateWithConfig(JCheckBox box, TrackedValue<Boolean> config) {
		syncStateWithConfigImpl(box, box::setSelected, box::isSelected, config);
	}

	private static void syncStateWithConfigImpl(
			AbstractButton button,
			Consumer<Boolean> buttonSetter, Supplier<Boolean> buttonGetter,
			TrackedValue<Boolean> config
	) {
		buttonSetter.accept(config.value());

		button.addActionListener(e -> {
			final boolean buttonValue = buttonGetter.get();
			if (buttonValue != config.value()) {
				config.setValue(buttonValue);
			}
		});

		config.registerCallback(updated -> {
			final boolean configValue = updated.value();
			if (configValue != buttonGetter.get()) {
				buttonSetter.accept(configValue);
			}
		});
	}

	/**
	 * @see #getCenteredFontBaseY(FontMetrics, int, int, int)
	 */
	public static int getCenteredFontBaseY(FontMetrics fontMetrics, int height, Insets insets) {
		return getCenteredFontBaseY(fontMetrics, height, insets.top, insets.bottom);
	}

	/**
	 * Calculates the baseline Y value for vertically centering the passed {@code fontMetrics}'
	 * {@linkplain FontMetrics#getFont() font} within a space with the passed
	 * {@code height}, {@code top} inset, and {@code bottom} inset.<br>
	 * Typically passed as the {@code y} parameter of {@link Graphics#drawString(String, int, int)}.
	 *
	 * @param fontMetrics the metrics used to calculate font space requirements
	 * @param height      the height of the space containing the font
	 * @param top         the top inset of the space containing the font
	 * @param bottom      the bottom inset of the space containing the font
	 *
	 * @return the Y value that places the baseline of the {@linkplain FontMetrics#getFont() font} such that it's
	 * centered within its containing space
	 */
	public static int getCenteredFontBaseY(FontMetrics fontMetrics, int height, int top, int bottom) {
		final int maxAscent = fontMetrics.getMaxAscent();
		final int maxDescent = fontMetrics.getMaxDescent();
		// simplified from:
		// final int availableY = height - top - bottom;
		// final int extraY = availableY - maxAscent - maxDescent;
		// return maxAscent + top + extraY / 2;
		return (maxAscent + top + height - bottom - maxDescent) / 2;
	}

	/**
	 * Creates a {@link JMenu} containing one {@linkplain JRadioButtonMenuItem radio item} for each value between the
	 * passed {@code min} and {@code max}, inclusive.
	 *
	 * <p> Listeners are added to keep the selected radio item and the passed {@code config}'s
	 * {@link TrackedValue#value() value} in sync.
	 *
	 * <p> Consider using {@link NumberInputDialog#promptInt} instead for large int ranges.
	 *
	 * @param gui            the gui the created menu will belong to
	 * @param translationKey the translation key for the created menu; the translation is
	 *                       {@linkplain I18n#translateFormatted(String, Object...) formatted} with the passed
	 *                       {@code config}'s {@link TrackedValue#value() value}
	 * @param config         the config value to sync with
	 * @param min            the minimum allowed value;
	 *                       this should coincide with any minimum imposed on the passed {@code config}
	 * @param max            the maximum allowed value;
	 *                       this should coincide with any maximum imposed on the passed {@code config}
	 *
	 * @return a newly created menu allowing configuration of the passed {@code config}
	 */
	public static SimpleEnigmaMenu createIntConfigRadioMenu(
			Gui gui, String translationKey,
			TrackedValue<Integer> config, int min, int max
	) {
		final SimpleEnigmaMenu menu = new SimpleEnigmaMenu(gui, translationKey, key -> I18n.translateFormatted(
				translationKey,
				config.value()
		));

		final Map<Integer, JRadioButtonMenuItem> radiosByChoice = IntStream.range(min, max + 1)
				.boxed()
				.collect(Collectors.toMap(
					Function.identity(),
					choice -> {
						final JRadioButtonMenuItem choiceItem = new JRadioButtonMenuItem();
						choiceItem.setText(Integer.toString(choice));
						if (choice.equals(config.value())) {
							choiceItem.setSelected(true);
						}

						choiceItem.addActionListener(e -> {
							if (!config.value().equals(choice)) {
								config.setValue(choice);
								menu.retranslate();
								gui.getMenuBar().clearSearchMenusResults();
							}
						});

						return choiceItem;
					}
				));

		final ButtonGroup choicesGroup = new ButtonGroup();
		for (final JRadioButtonMenuItem radio : radiosByChoice.values()) {
			choicesGroup.add(radio);
			menu.add(radio);
		}

		config.registerCallback(updated -> {
			final JRadioButtonMenuItem choiceItem = radiosByChoice.get(updated.value());

			if (!choiceItem.isSelected()) {
				choiceItem.setSelected(true);
				menu.retranslate();
				gui.getMenuBar().clearSearchMenusResults();
			}
		});

		return menu;
	}

	// based on JPopupMenu::getCurrentGraphicsConfiguration
	/**
	 * @return an {@link Optional} holding the {@link GraphicsConfiguration} of the
	 * {@linkplain GraphicsEnvironment#getScreenDevices() screen device} that contains the passed
	 * {@code x} and {@code y} coordinates if one could be found, or {@link Optional#empty()} otherwise
	 */
	public static Optional<GraphicsConfiguration> findGraphicsConfig(int x, int y) {
		for (final GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			if (device.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				final GraphicsConfiguration config = device.getDefaultConfiguration();
				if (config.getBounds().contains(x, y)) {
					return Optional.of(config);
				}
			}
		}

		return Optional.empty();
	}

	public enum FocusCondition {
		/**
		 * @see JComponent#WHEN_IN_FOCUSED_WINDOW
		 */
		WHEN_IN_FOCUSED_WINDOW(JComponent.WHEN_IN_FOCUSED_WINDOW),
		/**
		 * @see JComponent#WHEN_FOCUSED
		 */
		WHEN_FOCUSED(JComponent.WHEN_FOCUSED),
		/**
		 * @see JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		 */
		WHEN_ANCESTOR_OF_FOCUSED_COMPONENT(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		public static final ImmutableList<FocusCondition> VALUES = ImmutableList.copyOf(values());

		private final int value;

		FocusCondition(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}

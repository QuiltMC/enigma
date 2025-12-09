package org.quiltmc.enigma.gui.panel;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.docker.AllClassesDocker;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.docker.DeobfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.ObfuscatedClassesDocker;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Utils;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.quiltmc.enigma.gui.util.GuiUtil.consumeMousePositionOut;
import static org.quiltmc.enigma.gui.util.GuiUtil.getRecordIndexingService;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;

public class EntryTooltip extends JWindow {
	private static final int MOUSE_PAD = 5;
	private static final int SMALL_MOVE_THRESHOLD = 10;

	private static final int ROW_OUTER_INSET = 8;
	private static final int ROW_INNER_INSET = 2;

	private static final int DEFAULT_MAX_WIDTH = 600;

	private final Gui gui;
	private final JPanel content;

	private final AWTEventListener globalMouseListener = e -> {
		if (e instanceof MouseEvent mouseEvent) {
			final int id = mouseEvent.getID();
			if (id == MouseEvent.MOUSE_RELEASED) {
				EntryTooltip.this.dragStart = null;
			} else if (this.isVisible() && id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_CLICKED) {
				consumeMousePositionOut(this, ignored -> this.close());
			}
		}
	};

	private final AWTEventListener globalKeyListener = e -> {
		if (this.isShowing()) {
			final int id = e.getID();
			if (id == KeyEvent.KEY_TYPED) {
				this.closeAndDispatch(e);
			} else if (id == KeyEvent.KEY_PRESSED && e instanceof KeyEvent keyEvent) {
				final int modifiers = keyEvent.getModifiersEx();
				final int keyCode = keyEvent.getKeyCode();
				if (
						modifiers != 0 && keyCode != KeyEvent.VK_CONTROL
							// special case ctrl+c so an editor's copy doesn't overwrite text copied by a tooltip's copy
							&& !(keyCode == KeyEvent.VK_C && modifiers == InputEvent.CTRL_DOWN_MASK)
				) {
					this.closeAndDispatch(e);
				}
			}
		}
	};

	private final Set<Runnable> closeListeners = new HashSet<>();

	private int zoomAmount;
	private boolean repopulated;

	@Nullable
	private Point dragStart;

	@Nullable
	private DeclarationSnippetPanel declarationSnippet;

	@Nullable
	private Component eventReceiver;

	public EntryTooltip(Gui gui) {
		super(gui.getFrame());

		this.gui = gui;
		this.content = new JPanel(new FlexGridLayout());

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setLayout(new BorderLayout());

		this.setContentPane(this.content);
		this.content.setBorder(createLineBorder(Config.getCurrentSyntaxPaneColors().lineNumbersSelected.value()));

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (Config.editor().entryTooltips.interactable.value()) {
					EntryTooltip.this.dragStart = e.getButton() == MouseEvent.BUTTON1
							? new Point(e.getX(), e.getY())
							: null;

					e.consume();
				} else {
					// dispatching mouse events here causes cast exceptions when receivers get unexpected event sources
					EntryTooltip.this.close();
				}
			}
		});

		this.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				final Point dragStart = EntryTooltip.this.dragStart;
				if (dragStart != null) {
					final Point pos = EntryTooltip.this.getLocation();
					pos.translate(e.getX() - dragStart.x, e.getY() - dragStart.y);
					EntryTooltip.this.setLocation(pos);
				}
			}
		});

		this.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				EntryTooltip.this.close();
			}
		});
	}

	// setting/overriding max size does not work; getMaximumSize is never called
	@Override
	public Dimension getPreferredSize() {
		final Dimension superPreferred = super.getPreferredSize();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(
			Math.min(Math.min(superPreferred.width, screenSize.width / 2), ScaleUtil.scale(DEFAULT_MAX_WIDTH)),
			Math.min(superPreferred.height, screenSize.height / 2)
		);
	}

	// Sometimes when re-populating and resizing+moving, the cursor may be briefly over the parent EditorPanel.
	// This is used to stop EditorPanel from starting its mouseStoppedMovingTimer which may reset the tooltip to the
	// token under the cursor, discarding the re-populated content.
	public boolean hasRepopulated() {
		return this.repopulated;
	}

	/**
	 * Opens this tooltip and populates it with information about the passed {@code target}.
	 *
	 * @param target        the entry whose information will be displayed
	 * @param inherited     whether this tooltip is displaying information about the parent of another entry
	 * @param eventReceiver a component to receive events such as key presses that cause this tooltip to close;
	 *                      may be {@code null}
	 */
	public void open(Entry<?> target, boolean inherited, @Nullable Component eventReceiver) {
		this.eventReceiver = eventReceiver;
		this.populateWith(target, inherited, true);
		this.setVisible(true);

		this.addExternalListeners();
	}

	private void populateWith(Entry<?> target, boolean inherited, boolean opening) {
		this.repopulated = !opening;
		this.content.removeAll();

		final MouseAdapter stopInteraction = Config.editor().entryTooltips.interactable.value()
				? null : new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						// dispatching mouse events here causes cast exceptions when
						// receivers get unexpected event sources
						EntryTooltip.this.close();
						e.consume();
					}
				};

		final Font editorFont = ScaleUtil.scaleFont(Config.currentFonts().editor.value());
		final Font italEditorFont = ScaleUtil.scaleFont(Config.currentFonts().editor.value().deriveFont(Font.ITALIC));

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();

		{
			final Box parentLabelRow = Box.createHorizontalBox();

			final JLabel from = labelOf(
					I18n.translate(inherited ? "editor.tooltip.label.inherited_from" : "editor.tooltip.label.from"),
					italEditorFont
			);
			// the italics cause it to overlap with the colon if it has no right padding
			from.setBorder(createEmptyBorder(0, 0, 0, 1));
			parentLabelRow.add(from);
			parentLabelRow.add(colonLabelOf("", editorFont));

			parentLabelRow.add(this.parentLabelOf(target, editorFont, stopInteraction));

			parentLabelRow.setBorder(createEmptyBorder(ROW_OUTER_INSET, ROW_OUTER_INSET, ROW_INNER_INSET, ROW_OUTER_INSET));
			this.add(parentLabelRow, constraints.copy().alignCenterLeft());
		}

		final String javadoc = this.getJavadoc(target).orElse(null);
		final ImmutableList<ParamJavadoc> paramJavadocs =
				this.paramJavadocsOf(target, editorFont, italEditorFont, stopInteraction);
		if (javadoc != null || !paramJavadocs.isEmpty()) {
			this.add(new JSeparator(), constraints.nextRow().copy().fillX());

			final var javadocs = new JPanel(new FlexGridLayout());
			final FlexGridConstraints.Absolute javadocsConstraints = FlexGridConstraints.createAbsolute();

			if (javadoc != null) {
				final JTextArea javadocText = javadocOf(javadoc, italEditorFont, stopInteraction);
				javadocText.setBorder(createEmptyBorder(ROW_INNER_INSET, ROW_OUTER_INSET, ROW_INNER_INSET, ROW_OUTER_INSET));
				javadocs.add(javadocText, javadocsConstraints.copy().fillX());
			}

			if (!paramJavadocs.isEmpty()) {
				final JPanel params = new JPanel(new FlexGridLayout());

				final FlexGridConstraints.Absolute paramsConstraints = FlexGridConstraints.createAbsolute();

				for (final ParamJavadoc paramJavadoc : paramJavadocs) {
					params.add(paramJavadoc.name, paramsConstraints.copy().alignTopRight());

					params.add(paramJavadoc.javadoc, paramsConstraints.nextColumn().copy()
							.fillX()
							.alignTopLeft()
					);

					paramsConstraints.nextRow();
				}

				params.setBorder(createEmptyBorder(ROW_INNER_INSET, ROW_OUTER_INSET, ROW_INNER_INSET, ROW_OUTER_INSET));
				javadocs.add(params, javadocsConstraints.nextRow().copy().fillX());
			}

			final JScrollPane javadocsScroll = new JScrollPane(javadocs);
			javadocsScroll.setBorder(createEmptyBorder());
			this.add(javadocsScroll, constraints.nextRow().copy().fillX());
		}

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		{
			final ClassHandle targetTopClassHandle = this.gui.getController().getClassHandleProvider()
					.openClass(target.getTopLevelClass());

			if (targetTopClassHandle != null) {
				this.declarationSnippet = new DeclarationSnippetPanel(this.gui, target, targetTopClassHandle);

				this.declarationSnippet.offsetEditorZoom(this.zoomAmount);

				if (stopInteraction == null) {
					this.declarationSnippet.editor.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							if (e.getButton() == MouseEvent.BUTTON1) {
								EntryTooltip.this.declarationSnippet
										.consumeEditorMouseTarget((token, entry, resolvedParent) -> {
											EntryTooltip.this.onEntryClick(entry, e.getModifiersEx());
										}
								);
							}
						}
					});
				}

				{
					final Dimension oldSize = opening ? null : this.getSize();
					final Point oldMousePos = MouseInfo.getPointerInfo().getLocation();
					this.declarationSnippet.addSourceSetListener(source -> {
						this.repaint();

						if (this.declarationSnippet != null) {
							// without this, the editor gets focus and has a blue border
							// but only when it's in a scroll pane, for some reason
							this.declarationSnippet.ui.requestFocus();
						}

						// JTextAreas (javadocs) adjust their preferred sizes after the first pack, so pack twice
						this.pack();
						// There seems to be a race condition when packing twice in a row where
						// the tooltips window can be sized based on the first pack, but components are sized
						// based on the second pack.
						// Using invokeLater for *only* the second pack *seems* to solve it.
						SwingUtilities.invokeLater(this::pack);

						SwingUtilities.invokeLater(() -> {
							if (oldSize == null) {
								// opening
								if (oldMousePos.distance(MouseInfo.getPointerInfo().getLocation()) < SMALL_MOVE_THRESHOLD) {
									this.moveNearCursor();
								} else {
									this.moveOnScreen();
								}
							} else {
								// not opening
								this.moveMaintainingAnchor(oldMousePos, oldSize);
							}
						});
					});
				}

				if (stopInteraction != null) {
					this.declarationSnippet.editor.addMouseListener(stopInteraction);
				}

				this.add(this.declarationSnippet.ui, constraints.nextRow().copy()
						.fillX()
						.alignCenterLeft()
						.incrementPriority()
				);
			} else {
				this.add(new JSeparator(), constraints.nextRow().copy().fillX());

				final JLabel noSource = labelOf(I18n.translate("editor.tooltip.message.no_source"), italEditorFont);
				noSource.setBorder(createEmptyBorder(ROW_INNER_INSET, ROW_OUTER_INSET, ROW_INNER_INSET, ROW_OUTER_INSET));
				this.add(noSource, constraints.nextRow().copy().fillX());
			}
		}

		this.repaint();
		this.pack();

		if (opening) {
			this.moveNearCursor();
		} else {
			this.moveOnScreen();
		}
	}

	private Optional<String> getJavadoc(Entry<?> target) {
		final EntryRemapper remapper = this.gui.getController().getProject().getRemapper();
		return Optional
			.ofNullable(remapper.getMapping(target).javadoc())
			.or(() -> target instanceof MethodEntry targetMethod
				// try getting record field javadocs for record getters if the getter has no javadoc
				? getRecordIndexingService(this.gui)
					.map(service -> service.getComponentField(targetMethod))
					// this cast is required on java 17 for some reason
					.map(entry -> (FieldEntry) entry)
					.map(remapper::getMapping)
					.map(EntryMapping::javadoc)
				: Optional.empty()
			);
	}

	public void addCloseListener(Runnable listener) {
		this.closeListeners.add(listener);
	}

	public void removeCloseListener(Runnable listener) {
		this.closeListeners.remove(listener);
	}

	private void addExternalListeners() {
		Toolkit.getDefaultToolkit().addAWTEventListener(this.globalMouseListener, MouseEvent.MOUSE_EVENT_MASK);
		Toolkit.getDefaultToolkit().addAWTEventListener(this.globalKeyListener, AWTEvent.KEY_EVENT_MASK);
	}

	private void removeExternalListeners() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(this.globalMouseListener);
		Toolkit.getDefaultToolkit().removeAWTEventListener(this.globalKeyListener);
	}

	/**
	 * Moves this so it's near but not under the cursor, favoring the bottom right.
	 *
	 * <p> Also ensures this is entirely on-screen.
	 */
	private void moveNearCursor() {
		if (!this.isShowing()) {
			return;
		}

		final Dimension size = this.getSize();
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();

		final Point mousePos = MouseInfo.getPointerInfo().getLocation();

		final Insets screenInsets = GuiUtil.findGraphicsConfig(mousePos.x, mousePos.y)
				.map(toolkit::getScreenInsets)
				.orElse(new Insets(0, 0, 0, 0));

		final int x = findCoordinateSpace(
				size.width, screenInsets.left, screenSize.width - screenInsets.right,
				mousePos.x - MOUSE_PAD, mousePos.x + MOUSE_PAD
		);

		final int y = findCoordinateSpace(
				size.height, screenInsets.top, screenSize.height - screenInsets.bottom,
				mousePos.y - MOUSE_PAD, mousePos.y + MOUSE_PAD
		);

		this.setLocation(x, y);
	}

	/**
	 * After resizing, moves this so that the cursor is at an 'equivalent' relative location.
	 *
	 *  <p> The way the equivalent location is determined depends on whether this grew or shrunk, independently for the
	 *  {@code x} and {@code y} axes:
	 *  <ul>
	 *      <li> if this grew in an axis, then the distance between the cursor and the closest edge perpendicular to
	 *      that axis is maintained
	 *      <li> if this shrunk in an axis, then the ratio of the distances between the cursor and the two edges
	 *      perpendicular to that axis is maintained
	 *  </ul>
	 *
	 * <p> Also ensures this is entirely on-screen.
	 */
	private void moveMaintainingAnchor(Point oldMousePos, Dimension oldSize) {
		if (!this.isShowing()) {
			return;
		}

		final Point pos = this.getLocationOnScreen();

		final int left = oldMousePos.x - pos.x;
		final int top = oldMousePos.y - pos.y;

		final Dimension newSize = this.getSize();

		final int anchoredX;
		if (oldSize.width > newSize.width) {
			final int targetLeft = (int) ((double) (left * newSize.width) / oldSize.width);
			anchoredX = pos.x + left - targetLeft;
		} else {
			final int oldRight = pos.x + oldSize.width - oldMousePos.x;
			final int xDiff = left > oldRight ? oldSize.width - newSize.width : 0;
			anchoredX = pos.x + xDiff;
		}

		final int anchoredY;
		if (oldSize.height > newSize.height) {
			final int targetTop = (int) ((double) (top * newSize.height) / oldSize.height);
			anchoredY = pos.y + top - targetTop;
		} else {
			final int oldBottom = pos.y + oldSize.height - oldMousePos.y;
			final int yDiff = top > oldBottom ? oldSize.height - newSize.height : 0;
			anchoredY = pos.y + yDiff;
		}

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final Insets screenInsets = GuiUtil.findGraphicsConfig(pos.x, pos.y)
				.map(toolkit::getScreenInsets)
				.orElse(new Insets(0, 0, 0, 0));

		final int targetX = Utils.clamp(
				anchoredX, screenInsets.left,
				screenSize.width - screenInsets.right - newSize.width
		);
		final int targetY = Utils.clamp(
				anchoredY, screenInsets.top,
				screenSize.height - screenInsets.bottom - newSize.height
		);

		if (targetX != pos.x || targetY != pos.y) {
			this.setLocation(targetX, targetY);
		}
	}

	/**
	 * Ensures this is entirely on-screen.
	 */
	private void moveOnScreen() {
		if (!this.isShowing()) {
			return;
		}

		final Point pos = this.getLocationOnScreen();
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final Insets screenInsets = GuiUtil.findGraphicsConfig(pos.x, pos.y)
				.map(toolkit::getScreenInsets)
				.orElse(new Insets(0, 0, 0, 0));
		final Dimension size = this.getSize();

		final int offRight = pos.x + size.width - screenSize.width - screenInsets.right;
		final int x = Math.max(screenInsets.left, offRight > 0 ? pos.x - offRight : pos.x);

		final int offBottom = pos.y + size.height - screenSize.height - screenInsets.bottom;
		final int y = Math.max(screenInsets.top, offBottom > 0 ? pos.y - offBottom : pos.y);

		if (x != pos.x || y != pos.y) {
			this.setLocation(x, y);
		}
	}

	private void onEntryClick(Entry<?> entry, int modifiers) {
		if ((modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
			this.close();
			this.gui.getController().navigateTo(entry);
		} else {
			this.populateWith(entry, false, false);
		}
	}

	private static int findCoordinateSpace(int size, int screenMin, int screenMax, int mouseMin, int mouseMax) {
		final double spaceAfter = screenMax - mouseMax;
		if (spaceAfter >= size) {
			return mouseMax;
		} else {
			final int spaceBefore = mouseMin - size;
			if (spaceBefore >= screenMin) {
				return spaceBefore;
			} else {
				// doesn't fit before or after; align with screen edge that gives more space
				return spaceAfter < spaceBefore ? 0 : screenMax - size;
			}
		}
	}

	private static JLabel colonLabelOf(String text, Font font) {
		final JLabel label = labelOf(text + ":", font);
		label.setBorder(createEmptyBorder(0, 0, 0, 2));

		return label;
	}

	private static JLabel labelOf(String text, Font font) {
		final JLabel label = new JLabel(text);
		label.setFont(font);

		return label;
	}

	private static JTextArea javadocOf(String javadoc, Font font, MouseAdapter stopInteraction) {
		final JTextArea text = new JTextArea(javadoc);
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setForeground(Config.getCurrentSyntaxPaneColors().comment.value());
		text.setFont(font);
		text.setBackground(GuiUtil.TRANSPARENT);
		text.setCaretColor(GuiUtil.TRANSPARENT);
		text.getCaret().setSelectionVisible(true);
		text.setBorder(createEmptyBorder());

		if (stopInteraction != null) {
			text.addMouseListener(stopInteraction);
		}

		return text;
	}

	private static String translatePlaceholder(String key) {
		return "<%s>".formatted(I18n.translate(key));
	}

	private ImmutableList<ParamJavadoc> paramJavadocsOf(
			Entry<?> target, Font nameFont, Font javadocFont, MouseAdapter stopInteraction
	) {
		final EnigmaProject project = this.gui.getController().getProject();
		final EntryIndex entryIndex = project.getJarIndex().getIndex(EntryIndex.class);

		final Stream<Entry<?>> entries;
		if (target instanceof MethodEntry targetMethod) {
			entries = Optional
					.ofNullable(entryIndex.getDefinition(targetMethod))
					.stream()
					.flatMap(methodDef -> methodDef.getParameters(entryIndex).stream());
		} else if (target instanceof ClassEntry targetClass) {
			entries = getRecordIndexingService(this.gui)
					.stream()
					.flatMap(service -> service.streamComponentFields(targetClass));
		} else {
			entries = Stream.empty();
		}

		final EntryRemapper remapper = project.getRemapper();

		return entries
			.<ParamJavadoc>mapMulti((param, add) -> {
				final EntryMapping mapping = remapper.getMapping(param);
				if (mapping.javadoc() != null) {
					final JLabel name = colonLabelOf(remapper.deobfuscate(param).getSimpleName(), nameFont);
					final JTextArea javadoc = javadocOf(mapping.javadoc(), javadocFont, stopInteraction);

					add.accept(new ParamJavadoc(name, javadoc));
				}
			})
			.collect(toImmutableList());
	}

	public void close() {
		this.closeAndDispatch(null);
	}

	private void closeAndDispatch(@Nullable AWTEvent dispatching) {
		this.removeExternalListeners();

		this.repopulated = false;
		this.setVisible(false);
		this.content.removeAll();

		if (this.eventReceiver != null) {
			if (dispatching != null) {
				// this must be dispatched after setting not visible to avoid a stack overflow in the event dispatching
				this.eventReceiver.dispatchEvent(dispatching);
			}

			this.eventReceiver = null;
		}

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		this.closeListeners.forEach(Runnable::run);
	}

	private JLabel parentLabelOf(Entry<?> entry, Font font, @Nullable MouseAdapter stopInteraction) {
		final var nameBuilder = new StringBuilder();

		final Runnable tryDot = () -> {
			if (!nameBuilder.isEmpty()) {
				nameBuilder.insert(0, '.');
			}
		};

		final Entry<?> immediateParent = entry.getParent();
		Entry<?> parent = immediateParent;
		while (parent != null) {
			tryDot.run();

			nameBuilder.insert(0, this.getSimpleName(parent));

			parent = parent.getParent();
		}

		final ClassEntry topClass = entry.getTopLevelClass();

		final String packageName = this.gui.getController().getProject().getRemapper()
				.deobfuscate(topClass).getPackageName();
		if (packageName != null) {
			tryDot.run();

			nameBuilder.insert(0, packageName.replace('/', '.'));
		}

		final MouseListener parentClicked;
		if (stopInteraction == null) {
			if (immediateParent != null) {
				parentClicked = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						EntryTooltip.this.onEntryClick(immediateParent, e.getModifiersEx());
					}
				};
			} else {
				parentClicked = packageName == null ? null : this.createPackagedClickedListener(topClass);
			}
		} else {
			parentClicked = null;
		}

		final JLabel parentLabel = new JLabel(nameBuilder.isEmpty()
				? translatePlaceholder("editor.tooltip.label.no_package")
				: nameBuilder.toString()
		);

		final Font parentFont;
		if (parentClicked == null) {
			parentFont = font;
		} else {
			parentLabel.addMouseListener(parentClicked);

			@SuppressWarnings("rawtypes")
			final Map attributes = font.getAttributes();
			//noinspection unchecked
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			//noinspection unchecked
			parentFont = font.deriveFont(attributes);
		}

		parentLabel.setFont(parentFont);

		return parentLabel;
	}

	@Nullable
	private MouseListener createPackagedClickedListener(ClassEntry topClass) {
		final List<ClassesDocker> dockers = Stream
				.of(AllClassesDocker.class, DeobfuscatedClassesDocker.class, ObfuscatedClassesDocker.class)
				.<ClassesDocker>mapMulti((dockerClass, keep) -> {
					final ClassesDocker docker = EntryTooltip.this.gui.getDockerManager().getDocker(dockerClass);

					if (docker.getClassSelector().getPackageManager().getClassNode(topClass) != null) {
						keep.accept(docker);
					}
				})
				.toList();

		if (dockers.isEmpty()) {
			return null;
		}

		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
					final Set<Docker> activeDockers = new HashSet<>(
							EntryTooltip.this.gui.getDockerManager().getActiveDockers().values()
					);

					final List<ClassesDocker> sortedDockers = dockers.stream()
							// active first
							.sorted((left, right) -> {
								final boolean leftActive = activeDockers.contains(left);
								final boolean rightActive = activeDockers.contains(right);

								if (leftActive == rightActive) {
									return 0;
								} else {
									return leftActive ? -1 : 1;
								}
							})
							.toList();

					final String packageName = EntryTooltip.this.gui.getController().getProject().getRemapper()
							.deobfuscate(topClass)
							.getPackageName();

					for (final ClassesDocker docker : sortedDockers) {
						final ClassSelector selector = docker.getClassSelector();
						final TreePath path = selector.getPackageManager()
								.getPackagePathOrEmpty(packageName)
								.orElse(null);
						if (path != null) {
							EntryTooltip.this.close();
							EntryTooltip.this.gui.openDocker(docker.getClass());

							selector.setSelectionPath(path);
							selector.expandPath(path);
							selector.scrollPathToVisible(path);
							selector.requestFocus();

							return;
						}
					}
				}
			}
		};
	}

	private String getSimpleName(Entry<?> entry) {
		final EnigmaProject project = this.gui.getController().getProject();

		final String simpleObfName = entry.getSimpleName();
		if (!simpleObfName.isEmpty()) {
			if (entry instanceof MethodEntry method && method.isConstructor()) {
				return simpleObfName;
			} else if (Character.isJavaIdentifierStart(simpleObfName.charAt(0))) {
				final AccessFlags access = project.getJarIndex().getIndex(EntryIndex.class).getEntryAccess(entry);
				if (access == null || !(access.isSynthetic())) {
					return project.getRemapper().deobfuscate(entry).getSimpleName();
				} else {
					return translatePlaceholder("editor.tooltip.label.synthetic");
				}
			}
		}

		return translatePlaceholder("editor.tooltip.label.anonymous");
	}

	public void setZoom(int amount) {
		this.zoomAmount = amount;
	}

	public void resetZoom() {
		this.zoomAmount = 0;
	}

	private record ParamJavadoc(JLabel name, JTextArea javadoc) { }
}

package org.quiltmc.enigma.gui.panel;

import com.google.common.collect.ImmutableList;
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
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.tree.TreePath;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
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

	private final Set<Runnable> closeListeners = new HashSet<>();

	private int zoomAmount;
	private boolean repopulated;

	@Nullable
	private Point dragStart;

	@Nullable
	private DeclarationSnippetPanel declarationSnippet;

	public EntryTooltip(Gui gui) {
		super(gui.getFrame());

		this.gui = gui;
		this.content = new JPanel(new GridBagLayout());

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setLayout(new BorderLayout());

		this.setContentPane(this.content);
		this.content.setBorder(createLineBorder(Config.getCurrentSyntaxPaneColors().lineNumbersSelected.value()));

		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (e instanceof MouseEvent mouseEvent && mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
						EntryTooltip.this.dragStart = null;
					}
				},
				MouseEvent.MOUSE_RELEASED
		);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (Config.editor().entryTooltip.interactable.value()) {
					EntryTooltip.this.dragStart = e.getButton() == MouseEvent.BUTTON1
							? new Point(e.getX(), e.getY())
							: null;

					e.consume();
				} else {
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

		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (this.isShowing() && e.getID() == KeyEvent.KEY_TYPED) {
						this.close();
					}
				},
				AWTEvent.KEY_EVENT_MASK
		);

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
	 * @param target the entry whose information will be displayed
	 */
	public void open(Entry<?> target) {
		this.populateWith(target, true);
		this.setVisible(true);
	}

	private void populateWith(Entry<?> target, boolean opening) {
		this.repopulated = !opening;
		this.content.removeAll();

		@Nullable
		final MouseAdapter stopInteraction = Config.editor().entryTooltip.interactable.value() ? null : new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				EntryTooltip.this.close();
				e.consume();
			}
		};

		final Font editorFont = ScaleUtil.scaleFont(Config.currentFonts().editor.value());
		final Font italEditorFont = ScaleUtil.scaleFont(Config.currentFonts().editor.value().deriveFont(Font.ITALIC));

		int gridY = 0;

		{
			final Box parentLabelRow = Box.createHorizontalBox();

			final JLabel from = labelOf("from", italEditorFont);
			// the italics cause it to overlap with the colon if it has no right padding
			from.setBorder(createEmptyBorder(0, 0, 0, 1));
			parentLabelRow.add(from);
			parentLabelRow.add(colonLabelOf("", editorFont));

			parentLabelRow.add(this.parentLabelOf(target, editorFont, stopInteraction));
			parentLabelRow.add(Box.createHorizontalGlue());

			this.add(parentLabelRow, GridBagConstraintsBuilder.create()
					.pos(0, gridY++)
					.insets(ROW_OUTER_INSET, ROW_OUTER_INSET, ROW_INNER_INSET, ROW_OUTER_INSET)
					.anchor(GridBagConstraints.LINE_START)
					.build()
			);
		}

		final var mainContent = new JPanel(new GridBagLayout());
		// Put all main content in one big scroll pane.
		// Ideally there'd be separate javadoc and snippet scroll panes, but multiple scroll pane children
		// of a grid bag parent don't play nice when space is limited.
		// The snippet has its own scroll pane, but wrapping it in this one effectively disables its resizing.
		final var mainScroll = new JScrollPane(mainContent);
		mainScroll.setBorder(createEmptyBorder());
		int mainGridY = 0;

		final String javadoc = this.getJavadoc(target).orElse(null);
		final ImmutableList<ParamJavadoc> paramJavadocs =
				this.paramJavadocsOf(target, editorFont, italEditorFont, stopInteraction);
		if (javadoc != null || !paramJavadocs.isEmpty()) {
			mainContent.add(new JSeparator(), GridBagConstraintsBuilder.create()
					.pos(0, mainGridY++)
					.weightX(1)
					.fill(GridBagConstraints.HORIZONTAL)
					.build()
			);

			if (javadoc != null) {
				mainContent.add(javadocOf(javadoc, italEditorFont, stopInteraction), GridBagConstraintsBuilder.create()
						.pos(0, mainGridY++)
						.insets(ROW_INNER_INSET, ROW_OUTER_INSET)
						.weightX(1)
						.fill(GridBagConstraints.HORIZONTAL)
						.anchor(GridBagConstraints.LINE_START)
						.build()
				);
			}

			if (!paramJavadocs.isEmpty()) {
				final JPanel params = new JPanel(new GridBagLayout());
				int paramsGridY = 0;

				for (final ParamJavadoc paramJavadoc : paramJavadocs) {
					params.add(paramJavadoc.name, GridBagConstraintsBuilder.create()
							.pos(0, paramsGridY)
							.anchor(GridBagConstraints.FIRST_LINE_END)
							.build()
					);

					params.add(paramJavadoc.javadoc, GridBagConstraintsBuilder.create()
							.pos(1, paramsGridY++)
							.weightX(1)
							.fill(GridBagConstraints.HORIZONTAL)
							.anchor(GridBagConstraints.LINE_START)
							.build()
					);
				}

				mainContent.add(params, GridBagConstraintsBuilder.create()
						.insets(ROW_INNER_INSET, ROW_OUTER_INSET)
						.pos(0, mainGridY++)
						.weightX(1)
						.fill(GridBagConstraints.HORIZONTAL)
						.build()
				);
			}
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
								EntryTooltip.this.declarationSnippet.consumeEditorMouseTarget((token, entry) -> {
									EntryTooltip.this.onEntryClick(entry, e.getModifiersEx());
								});
							}
						}
					});
				}

				{
					final Dimension oldSize = opening ? null : this.getSize();
					final Point oldMousePos = MouseInfo.getPointerInfo().getLocation();
					this.declarationSnippet.addSourceSetListener(source -> {
						this.pack();
						// swing </3
						// a second call is required to eliminate extra space
						this.pack();

						if (this.declarationSnippet != null) {
							// without this, the editor gets focus and has a blue border
							// but only when it's in a scroll pane, for some reason
							this.declarationSnippet.ui.requestFocus();
						}

						final JScrollBar vertical = mainScroll.getVerticalScrollBar();
						// scroll to bottom so declaration snippet is in view
						vertical.setValue(vertical.getMaximum());

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
				}

				if (stopInteraction != null) {
					this.declarationSnippet.editor.addMouseListener(stopInteraction);
				}

				mainContent.add(this.declarationSnippet.ui, GridBagConstraintsBuilder.create()
						.pos(0, mainGridY++)
						.weightX(1)
						.fill(GridBagConstraints.HORIZONTAL)
						.anchor(GridBagConstraints.LINE_START)
						.build()
				);
			} else {
				mainContent.add(new JSeparator(), GridBagConstraintsBuilder.create()
						.pos(0, mainGridY++)
						.weightX(1)
						.fill(GridBagConstraints.HORIZONTAL)
						.build()
				);

				mainContent.add(labelOf("No source available", italEditorFont), GridBagConstraintsBuilder.create()
						.pos(0, mainGridY++)
						.weightX(1)
						.fill(GridBagConstraints.HORIZONTAL)
						.anchor(GridBagConstraints.LINE_START)
						.insets(ROW_INNER_INSET, ROW_OUTER_INSET)
						.build()
				);
			}
		}

		this.add(mainScroll, GridBagConstraintsBuilder.create()
				.pos(0, gridY++)
				.weight(1, 1)
				.fill(GridBagConstraints.BOTH)
				.build()
		);

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
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Point mousePos = MouseInfo.getPointerInfo().getLocation();

		final int x = findCoordinateSpace(
				size.width, screenSize.width,
				mousePos.x - MOUSE_PAD, mousePos.x + MOUSE_PAD
		);

		final int y = findCoordinateSpace(
				size.height, screenSize.height,
				mousePos.y - MOUSE_PAD, mousePos.y + MOUSE_PAD
		);

		this.setLocation(x, y);
	}

	/**
	 * After resizing, moves this so that the old distance between the cursor and the closest corner remains the same.
	 *
	 * <p> Also ensures this is entirely on-screen.
	 */
	private void moveMaintainingAnchor(Point oldMousePos, Dimension oldSize) {
		if (!this.isShowing()) {
			return;
		}

		final Point pos = this.getLocationOnScreen();

		final int oldLeft = oldMousePos.x - pos.x;
		final int oldRight = pos.x + oldSize.width - oldMousePos.x;
		final boolean anchorRight = oldLeft > oldRight;

		final int oldTop = oldMousePos.y - pos.y;
		final int oldBottom = pos.y + oldSize.height - oldMousePos.y;
		final boolean anchorBottom = oldTop > oldBottom;

		if (anchorRight || anchorBottom) {
			final Dimension newSize = this.getSize();

			final int xDiff = anchorRight ? oldSize.width - newSize.width : 0;
			final int yDiff = anchorBottom ? oldSize.height - newSize.height : 0;

			this.setLocation(pos.x + xDiff, pos.y + yDiff);
		}

		this.moveOnScreen();
	}

	/**
	 * Ensures this is entirely on-screen.
	 */
	private void moveOnScreen() {
		if (!this.isShowing()) {
			return;
		}

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Dimension size = this.getSize();
		final Point pos = this.getLocationOnScreen();

		final int xOffScreen = pos.x + size.width - screenSize.width;
		final int yOffScreen = pos.y + size.height - screenSize.height;

		final boolean moveX = xOffScreen > 0;
		final boolean moveY = yOffScreen > 0;

		if (moveX || moveY) {
			final int x = pos.x - (moveX ? xOffScreen : 0);
			final int y = pos.y - (moveY ? yOffScreen : 0);

			this.setLocation(x, y);
		}
	}

	private void onEntryClick(Entry<?> entry, int modifiers) {
		if ((modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
			this.close();
			this.gui.getController().navigateTo(entry);
		} else {
			this.populateWith(entry, false);
		}
	}

	private static int findCoordinateSpace(int size, int screenSize, int mouseMin, int mouseMax) {
		final double spaceAfter = screenSize - mouseMax;
		if (spaceAfter >= size) {
			return mouseMax;
		} else {
			final int spaceBefore = mouseMin - size;
			if (spaceBefore >= 0) {
				return spaceBefore;
			} else {
				// doesn't fit before or after; align with screen edge that gives more space
				return spaceAfter < spaceBefore ? 0 : screenSize - size;
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
		text.setBackground(invisibleColorOf());
		text.setCaretColor(invisibleColorOf());
		text.getCaret().setSelectionVisible(true);
		text.setBorder(createEmptyBorder());

		if (stopInteraction != null) {
			text.addMouseListener(stopInteraction);
		}

		return text;
	}

	private static Color invisibleColorOf() {
		return new Color(0, 0, 0, 0);
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
		this.repopulated = false;
		this.setVisible(false);
		this.content.removeAll();

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

		@Nullable
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

		final JLabel parentLabel = new JLabel(nameBuilder.isEmpty() ? "<no package>" : nameBuilder.toString());

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
					return "<synthetic>";
				}
			}
		}

		return "<anonymous>";
	}

	public void setZoom(int amount) {
		this.zoomAmount = amount;
	}

	public void resetZoom() {
		this.zoomAmount = 0;
	}

	private record ParamJavadoc(JLabel name, JTextArea javadoc) { }
}

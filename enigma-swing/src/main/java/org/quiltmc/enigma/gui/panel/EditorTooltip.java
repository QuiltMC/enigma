package org.quiltmc.enigma.gui.panel;

import com.google.common.collect.ImmutableList;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;

public class EditorTooltip extends JWindow {
	private static final int MOUSE_PAD = 5;
	private static final int SMALL_MOVE_THRESHOLD = 10;

	private static final int OUTER_ROW_PAD = 8;
	private static final int INNER_ROW_PAD = 2;

	private static Border topRowInsetsOf() {
		return createEmptyBorder(OUTER_ROW_PAD, OUTER_ROW_PAD, INNER_ROW_PAD, OUTER_ROW_PAD);
	}

	private static Border innerRowInsetsOf() {
		return createEmptyBorder(INNER_ROW_PAD, OUTER_ROW_PAD, INNER_ROW_PAD, OUTER_ROW_PAD);
	}

	private final Gui gui;
	private final Box content;

	@Nullable
	private Point dragStart;

	@Nullable
	private DeclarationSnippetPanel declarationSnippet;

	// TODO clamp size
	public EditorTooltip(Gui gui) {
		super();

		this.gui = gui;
		this.content = new Box(BoxLayout.PAGE_AXIS);

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setLayout(new BorderLayout());

		this.setContentPane(this.content);
		this.content.setBorder(createLineBorder(Config.getCurrentSyntaxPaneColors().lineNumbersSelected.value()));

		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (e instanceof MouseEvent mouseEvent && mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
						EditorTooltip.this.dragStart = null;
					}
				},
				MouseEvent.MOUSE_RELEASED
		);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (Config.editor().tooltip.interactable.value()) {
					EditorTooltip.this.dragStart = e.getButton() == MouseEvent.BUTTON1
							? new Point(e.getX(), e.getY())
							: null;

					e.consume();
				} else {
					EditorTooltip.this.close();
				}
			}
		});

		this.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				final Point dragStart = EditorTooltip.this.dragStart;
				if (dragStart != null) {
					final Point pos = EditorTooltip.this.getLocation();
					pos.translate(e.getX() - dragStart.x, e.getY() - dragStart.y);
					EditorTooltip.this.setLocation(pos);
				}
			}
		});
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
		this.content.removeAll();

		@Nullable
		final MouseAdapter stopInteraction = Config.editor().tooltip.interactable.value() ? null : new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				EditorTooltip.this.close();
				e.consume();
			}
		};

		final Font editorFont = Config.currentFonts().editor.value();
		final Font italEditorFont = editorFont.deriveFont(Font.ITALIC);

		this.add(rowOf(row -> {
			row.setBorder(topRowInsetsOf());
			// TODO add stat icon if enabled
			// TODO add class/record/enum/method icon
			final JLabel from = labelOf("from", italEditorFont);
			// the italics cause it to overlap with the colon if it has no right padding
			from.setBorder(createEmptyBorder(0, 0, 0, 1));
			row.add(from);
			row.add(colonLabelOf(""));
			final Font parentFont;
			@Nullable
			final MouseListener parentClicked;
			final Entry<?> parent = target.getParent();
			if (stopInteraction == null && parent != null) {
				@SuppressWarnings("rawtypes")
				final Map attributes = editorFont.getAttributes();
				//noinspection unchecked
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				//noinspection unchecked
				parentFont = editorFont.deriveFont(attributes);
				parentClicked = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						EditorTooltip.this.navigateOnClick(parent, e.getModifiersEx());
					}
				};
			} else {
				parentFont = editorFont;
				parentClicked = null;
			}

			final JLabel parentLabel = labelOf(this.getParentName(target).orElse("<no package>"), parentFont);

			if (parentClicked != null) {
				parentLabel.addMouseListener(parentClicked);
			}

			row.add(parentLabel);
		}));

		// TODO make javadocs and snippet copyable
		final String javadoc = this.gui.getController().getProject().getRemapper().getMapping(target).javadoc();
		final ImmutableList<ParamJavadoc> paramJavadocs = this.paramJavadocsOf(target, italEditorFont, stopInteraction);
		if (javadoc != null || !paramJavadocs.isEmpty()) {
			this.add(new JSeparator());

			if (javadoc != null) {
				this.add(rowOf(row -> {
					row.setBorder(innerRowInsetsOf());
					row.add(javadocOf(javadoc, italEditorFont, stopInteraction));
				}));
			}

			if (!paramJavadocs.isEmpty()) {
				// TODO for some reason the param grid has extra space above and below it
				final JPanel params = new JPanel(new GridBagLayout());
				params.setBorder(createEmptyBorder(2, 2, 2, 2));

				final GridBagConstraints nameConstraints = new GridBagConstraints();
				nameConstraints.gridx = 0;
				nameConstraints.anchor = GridBagConstraints.FIRST_LINE_END;

				final GridBagConstraints javadocConstraints = new GridBagConstraints();
				javadocConstraints.gridx = 1;
				javadocConstraints.fill = GridBagConstraints.HORIZONTAL;
				javadocConstraints.weightx = 1;
				javadocConstraints.anchor = GridBagConstraints.LINE_START;

				for (final ParamJavadoc paramJavadoc : paramJavadocs) {
					params.add(paramJavadoc.name, nameConstraints);
					params.add(paramJavadoc.javadoc, javadocConstraints);
				}

				this.add(rowOf(row -> {
					row.setBorder(innerRowInsetsOf());
					row.add(params);
				}));
			}
		}

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
			this.declarationSnippet = null;
		}

		{
			final ClassHandle targetTopClassHandle = this.gui.getController().getClassHandleProvider()
					.openClass(target.getTopLevelClass());

			final Component sourceInfo;
			if (targetTopClassHandle != null) {
				// TODO expand right to fill width
				this.declarationSnippet = new DeclarationSnippetPanel(this.gui, target, targetTopClassHandle);

				this.declarationSnippet.editor.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							EditorTooltip.this.declarationSnippet.consumeEditorMouseTarget((token, entry) -> {
								EditorTooltip.this.navigateOnClick(entry, e.getModifiersEx());
							});
						}
					}
				});

				{
					final Dimension oldSize = opening ? null : this.getSize();
					final Point oldMousePos = MouseInfo.getPointerInfo().getLocation();
					this.declarationSnippet.addSourceSetListener(source -> {
						this.pack();

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

				sourceInfo = this.declarationSnippet.ui;
			} else {
				sourceInfo = labelOf("No source available", italEditorFont);
			}

			this.add(rowOf(sourceInfo));
		}

		this.pack();

		if (opening) {
			this.moveNearCursor();
		} else {
			this.moveOnScreen();
		}
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
		final boolean anchorRight = oldLeft >= oldRight;

		final int oldTop = oldMousePos.y - pos.y;
		final int oldBottom = pos.y + oldSize.height - oldMousePos.y;
		final boolean anchorBottom = oldTop >= oldBottom;

		if (anchorRight || anchorBottom) {
			final Dimension newSize = this.getSize();

			final int x;
			if (anchorRight) {
				final int widthDiff = oldSize.width - newSize.width;
				x = pos.x + widthDiff;
			} else {
				x = pos.x;
			}

			final int y;
			if (anchorBottom) {
				final int heightDiff = oldSize.height - newSize.height;
				y = pos.y + heightDiff;
			} else {
				y = pos.y;
			}

			this.setLocation(x, y);
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

	private void navigateOnClick(Entry<?> entry, int modifiers) {
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

	private static JLabel colonLabelOf(String text) {
		final JLabel label = labelOf(text + ":", Config.currentFonts().editor.value());
		label.setBorder(createEmptyBorder(0, 0, 0, 2));

		return label;
	}

	private static JLabel labelOf(String text, Font font) {
		final JLabel label = new JLabel(text);
		label.setFont(font);

		return label;
	}

	// TODO for some reason sometimes there's extra space below the text, about one line's worth
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

	private ImmutableList<ParamJavadoc> paramJavadocsOf(Entry<?> target, Font font, MouseAdapter stopInteraction) {
		final EnigmaProject project = this.gui.getController().getProject();
		final EntryIndex entryIndex = project.getJarIndex().getIndex(EntryIndex.class);

		if (target instanceof MethodEntry targetMethod) {
			final MethodDefEntry methodDef = entryIndex.getDefinition(targetMethod);
			if (methodDef == null) {
				return ImmutableList.of();
			} else {
				final EntryRemapper remapper = project.getRemapper();

				return methodDef.getParameters(entryIndex).stream()
					.<ParamJavadoc>mapMulti((param, add) -> {
						final EntryMapping mapping = remapper.getMapping(param);
						if (mapping.javadoc() != null) {
							final JLabel name = colonLabelOf(remapper.deobfuscate(param).getSimpleName());
							final JTextArea javadoc = javadocOf(mapping.javadoc(), font, stopInteraction);

							add.accept(new ParamJavadoc(name, javadoc));
						}
					})
					.collect(toImmutableList());
			}
		} else {
			// TODO add record component javadocs once there's a RecordIndex
			return ImmutableList.of();
		}
	}

	private static Box rowOf(Component... components) {
		return rowOf(row -> {
			for (final Component component : components) {
				row.add(component);
			}
		});
	}

	private static Box rowOf(Consumer<Box> rowInitializer) {
		final Box row = Box.createHorizontalBox();
		rowInitializer.accept(row);
		row.add(Box.createHorizontalGlue());
		// row.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		return row;
	}

	public void close() {
		this.setVisible(false);
		this.content.removeAll();

		if (this.declarationSnippet != null) {
			this.declarationSnippet.classHandler.removeListener();
		}
	}

	private Optional<String> getParentName(Entry<?> entry) {
		final var builder = new StringBuilder();

		final Runnable tryDot = () -> {
			if (!builder.isEmpty()) {
				builder.insert(0, '.');
			}
		};

		Entry<?> parent = entry.getParent();
		while (parent != null) {
			tryDot.run();

			builder.insert(0, this.getSimpleName(parent));

			parent = parent.getParent();
		}

		final String packageName = entry.getTopLevelClass().getPackageName();
		if (packageName != null) {
			tryDot.run();

			builder.insert(0, packageName.replace('/', '.'));
		}

		return builder.isEmpty() ? Optional.empty() : Optional.of(builder.toString());
	}

	private String getSimpleName(Entry<?> entry) {
		final EnigmaProject project = this.gui.getController().getProject();

		final String simpleObfName = entry.getSimpleName();
		if (!simpleObfName.isEmpty() && Character.isJavaIdentifierStart(simpleObfName.charAt(0))) {
			final AccessFlags access = project.getJarIndex().getIndex(EntryIndex.class).getEntryAccess(entry);
			if (access == null || !(access.isSynthetic())) {
				return project.getRemapper().deobfuscate(entry).getSimpleName();
			} else {
				return "<synthetic>";
			}
		}

		return "<anonymous>";
	}

	private record ParamJavadoc(JLabel name, JTextArea javadoc) { }
}

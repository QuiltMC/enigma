package org.quiltmc.enigma.gui.panel;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.util.concurrent.Runnables;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindToolBar;
import org.quiltmc.enigma.gui.element.EditorPopupMenu;
import org.quiltmc.enigma.gui.element.NavigatorPanel;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.syntaxpain.DefaultSyntaxAction;
import org.quiltmc.syntaxpain.SyntaxDocument;
import org.quiltmc.enigma.gui.highlight.SelectionHighlightPainter;
import org.quiltmc.enigma.util.LineIndexer;
import org.quiltmc.enigma.util.Result;
import org.quiltmc.syntaxpain.LineNumbersRuler;
import org.tinylog.Logger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.text.JTextComponent;

import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public class EditorPanel extends BaseEditorPanel {
	private static final int MOUSE_STOPPED_MOVING_DELAY = 100;
	private static final Pattern CLASS_PUNCTUATION = Pattern.compile("/|\\$");

	private final NavigatorPanel navigatorPanel;
	private final EnigmaQuickFindToolBar quickFindToolBar = new EnigmaQuickFindToolBar();
	private final EditorPopupMenu popupMenu;

	// DIY tooltip because JToolTip can't be moved or resized
	private final JWindow tooltip = new JWindow();

	@Nullable
	private Token lastMouseTargetToken;

	// avoid finding the mouse entry every mouse movement update
	private final Timer mouseStoppedMovingTimer = new Timer(MOUSE_STOPPED_MOVING_DELAY, e -> {
		this.consumeEditorMouseTarget(
				(targetToken, targetEntry) -> {
					this.hideTokenTooltipTimer.restart();
					if (this.tooltip.isVisible()) {
						this.showTokenTooltipTimer.stop();

						if (!targetToken.equals(this.lastMouseTargetToken)) {
							this.lastMouseTargetToken = targetToken;
							this.updateToolTip(targetEntry);
						}
					} else {
						this.lastMouseTargetToken = targetToken;
						this.showTokenTooltipTimer.start();
					}
				},
				() -> {
					this.lastMouseTargetToken = null;
					this.showTokenTooltipTimer.stop();
				}
		);
	});
	private final Timer showTokenTooltipTimer = new Timer(
			ToolTipManager.sharedInstance().getInitialDelay() - MOUSE_STOPPED_MOVING_DELAY, e -> {
				this.consumeEditorMouseTarget((targetToken, targetEntry) -> {
					this.hideTokenTooltipTimer.restart();
					if (targetToken.equals(this.lastMouseTargetToken)) {
						this.tooltip.setVisible(true);
						this.updateToolTip(targetEntry);
					}
				});
			}
	);
	// TODO stop hide timer when mouse is over tooltip or target token
	// TODO tooltip re-shows after short delay after hiding
	private final Timer hideTokenTooltipTimer = new Timer(
			ToolTipManager.sharedInstance().getDismissDelay() - MOUSE_STOPPED_MOVING_DELAY,
			e -> this.closeTooltip()
	);

	private final List<EditorActionListener> listeners = new ArrayList<>();

	public EditorPanel(Gui gui, NavigatorPanel navigator) {
		super(gui);

		this.navigatorPanel = navigator;

		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot()));

		// HACK to prevent DefaultCaret from calling setSelectionVisible(false) when quickFind gains focus
		if (this.editor.getCaret() instanceof FocusListener caretFocusListener) {
			this.editor.removeFocusListener(caretFocusListener);
			this.editor.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					caretFocusListener.focusGained(e);
				}

				@Override
				public void focusLost(FocusEvent e) {
					EditorPanel.this.editor.getCaret().setVisible(false);
					EditorPanel.this.editor.getCaret().setSelectionVisible(
							EditorPanel.this.editor.hasFocus() || EditorPanel.this.quickFindToolBar.isVisible()
					);
				}
			});
		}

		this.quickFindToolBar.setVisible(false);
		// init editor popup menu
		this.popupMenu = new EditorPopupMenu(this, gui);

		// global listener so tooltip hides even if clicking outside editor
		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (e.getID() == MouseEvent.MOUSE_PRESSED) {
						this.closeTooltip();
					}
				},
				MouseEvent.MOUSE_PRESSED
		);

		final MouseAdapter editorMouseAdapter = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiersEx() & CTRL_DOWN_MASK) != 0 && e.getButton() == MouseEvent.BUTTON1) {
					// ctrl + left click
					EditorPanel.this.navigateToCursorReference();
				}
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				EditorPanel.this.tooltip.setVisible(false);
				EditorPanel.this.mouseStoppedMovingTimer.stop();
				EditorPanel.this.showTokenTooltipTimer.stop();
				EditorPanel.this.hideTokenTooltipTimer.stop();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
					case MouseEvent.BUTTON3 -> // Right click
						EditorPanel.this.editor.setCaretPosition(EditorPanel.this.editor.viewToModel2D(e.getPoint()));
					case 4 -> // Back navigation
						gui.getController().openPreviousReference();
					case 5 -> // Forward navigation
						gui.getController().openNextReference();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				EditorPanel.this.mouseStoppedMovingTimer.restart();
			}
		};

		this.editor.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				EditorPanel.this.closeTooltip();
			}
		});

		this.editor.addMouseListener(editorMouseAdapter);
		this.editor.addMouseMotionListener(editorMouseAdapter);
		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot()));

		this.mouseStoppedMovingTimer.setRepeats(false);
		this.showTokenTooltipTimer.setRepeats(false);
		this.hideTokenTooltipTimer.setRepeats(false);

		this.tooltip.setVisible(false);
		this.tooltip.setAlwaysOnTop(true);
		this.tooltip.setType(Window.Type.POPUP);
		this.tooltip.setLayout(new BorderLayout());
		this.tooltip.setContentPane(new Box(BoxLayout.PAGE_AXIS));

		this.tooltip.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				consumeMousePositionIn(EditorPanel.this.editor, (absolutMousePosition, editorMousePosition) -> {
					final MouseEvent editorMouseEvent = new MouseEvent(
							EditorPanel.this.editor, e.getID(), e.getWhen(), e.getModifiersEx(),
							editorMousePosition.x, editorMousePosition.y,
							absolutMousePosition.x, absolutMousePosition.y,
							e.getClickCount(), e.isPopupTrigger(), e.getButton()
					);

					for (final MouseListener listener : EditorPanel.this.editor.getMouseListeners()) {
						listener.mousePressed(editorMouseEvent);
						if (editorMouseEvent.isConsumed()) {
							break;
						}
					}
				});

				e.consume();
			}
		});

		this.editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent event) {
				EntryReference<Entry<?>, Entry<?>> ref = EditorPanel.this.getCursorReference();
				if (ref == null) return;
				if (!EditorPanel.this.controller.getProject().isRenamable(ref)) return;

				if (!event.isControlDown() && !event.isAltDown() && Character.isJavaIdentifierStart(event.getKeyChar())) {
					EnigmaProject project = gui.getController().getProject();
					EntryReference<Entry<?>, Entry<?>> reference = project.getRemapper().deobfuscate(EditorPanel.this.cursorReference);
					Entry<?> entry = reference.getNameableEntry();

					String name = String.valueOf(event.getKeyChar());
					if (entry instanceof ClassEntry classEntry && classEntry.getParent() == null) {
						String packageName = classEntry.getPackageName();
						if (packageName != null) {
							name = packageName + "/" + name;
						}
					}

					gui.startRename(EditorPanel.this, name);
				}
			}
		});

		this.reloadKeyBinds();
		this.addSourceSetListener(source -> {
			if (this.navigatorPanel != null) {
				for (Entry<?> entry : source.getIndex().declarations()) {
					this.navigatorPanel.addEntry(entry);
				}
			}
		});

		this.ui.putClientProperty(EditorPanel.class, this);
	}

	private void closeTooltip() {
		EditorPanel.this.tooltip.setVisible(false);
		EditorPanel.this.lastMouseTargetToken = null;
		EditorPanel.this.mouseStoppedMovingTimer.stop();
		EditorPanel.this.showTokenTooltipTimer.stop();
		EditorPanel.this.hideTokenTooltipTimer.stop();
	}

	private void updateToolTip(Entry<?> target) {
		final Container tooltipContent = this.tooltip.getContentPane();
		tooltipContent.removeAll();

		final Entry<?> deobfTarget = this.gui.getController().getProject().getRemapper().deobfuscate(target);

		tooltipContent.add(new JLabel(deobfTarget.getFullName()));
		if (target instanceof ParentedEntry<?> parentedTarget) {
			final ClassEntry targetTopClass = parentedTarget.getTopLevelClass();

			final ClassHandle targetTopClassHandle = targetTopClass.equals(this.getSource().getEntry())
					? this.classHandle
					: this.gui.getController().getClassHandleProvider().openClass(targetTopClass);

			if (targetTopClassHandle != null) {
				final BaseEditorPanel tooltipEditor = new BaseEditorPanel(this.gui);

				Optional.ofNullable(tooltipEditor.editorScrollPane.getRowHeader())
						.map(JViewport::getView)
						// LineNumbersRuler is installed by syntaxpain
						.map(view -> view instanceof LineNumbersRuler lineNumbers ? lineNumbers : null)
						// TODO offset line numbers instead of removing them once
						//  offsets are implemented in syntaxpain
						.ifPresent(lineNumbers -> lineNumbers.deinstall(tooltipEditor.editor));

				tooltipEditor.addSourceSetListener(source -> {
					final Token declarationToken = source.getIndex().getDeclarationToken(target);
					if (declarationToken != null) {
						this.tooltip.pack();

						// TODO create custom highlighter
						tooltipEditor.navigateToToken(declarationToken, SelectionHighlightPainter.INSTANCE);
					}
				});

				tooltipEditor.getEditor().setEditable(false);
				tooltipEditor.setClassHandle(targetTopClassHandle, source -> this
						.createTrimmedBounds(source, target, deobfTarget)
				);
				tooltipContent.add(tooltipEditor.ui);
			}
		}

		this.tooltip.setLocation(MouseInfo.getPointerInfo().getLocation());

		this.tooltip.pack();
	}

	private Result<TrimmedBounds, String> findClassBounds(DecompiledClassSource source, ClassEntry target, String targetName) {
		return this.getNodeType(target, targetName).andThen(nodeType -> {
			final String sourceString = source.toString();
			final ParseResult<CompilationUnit> parseResult = parse(sourceString);
			return parseResult
				.getResult()
				.map(unit -> unit
					.findFirst(nodeType, declaration -> declaration
						.getFullyQualifiedName()
						.filter(name -> name.equals(targetName))
						.isPresent()
					)
					.map(targetDeclaration -> targetDeclaration
						.getRange()
						.map(range -> targetDeclaration
							.getTokenRange()
							.map(TokenRange::iterator)
							.map(tokenItr -> {
								while (tokenItr.hasNext()) {
									final JavaToken javaToken = tokenItr.next();
									if (javaToken.asString().equals("{")) {
										return javaToken.getRange()
											.map(openRange -> Result.<TrimmedBounds, String>ok(
													toTrimmedBounds(new LineIndexer(sourceString), range.begin, openRange.begin)
											))
											.orElseGet(() -> Result.err("No open curly brace range for %s!".formatted(targetName)));
									}
								}

								return Result.<TrimmedBounds, String>err("No open curly brace for %s!".formatted(targetName));
							})
							.orElseGet(() -> Result.err("No token range for %s!".formatted(targetName)))
						)
						.orElseGet(() -> Result.err("No declaration range for %s!".formatted(targetName)))
					)
					.orElseGet(() -> Result.err("Failed to find %s in parsed source!".formatted(targetName)))
				)
				.orElseGet(() -> Result.err("Failed to parse source: " + parseResult.getProblems()));
		});
	}

	private Result<TrimmedBounds, String> findMethodBounds(
			DecompiledClassSource source, MethodEntry target,
			String targetName, String targetSimpleName
	) {
		final String sourceString = source.toString();

		final ParseResult<CompilationUnit> parseResult = parse(sourceString);
			return parseResult
				.getResult()
				.map(unit -> {
					final LineIndexer lineIndexer = new LineIndexer(sourceString);
					final Token targetToken = source.getIndex().getDeclarationToken(target);

					return unit
						.findAll(MethodDeclaration.class, declaration -> {
							if (declaration.getNameAsString().equals(targetSimpleName) && declaration.hasRange()) {
								final Range range = declaration.getRange().orElseThrow();

								return lineIndexer.getIndex(range.begin) <= targetToken.start
									&& lineIndexer.getIndex(range.end) >= targetToken.end;
							} else {
								return false;
							}
						})
						.stream()
						// deepest first
						.min(Comparator.comparingInt(
								// hasRange() already checked in filter
								declaration -> lineIndexer.getIndex(declaration.getRange().orElseThrow().end)
						))
						.map(targetDeclaration -> {
								// hasRange() already checked in filter
								final Range range = targetDeclaration.getRange().orElseThrow();

							return targetDeclaration
									.getBody()
									.map(body -> body.getRange()
										.map(bodyRange -> Result.<TrimmedBounds, String>ok(
											toTrimmedBounds(lineIndexer, range.begin, bodyRange.begin)
										))
										.orElseGet(() -> Result.err("No body range for %s!".formatted(targetName)))
									)
									// no body: abstract
									.orElseGet(() -> Result.ok(toTrimmedBounds(lineIndexer, range.begin, range.end)));
							}
						)
						.orElseGet(() -> Result.err("Failed to find %s in parsed source!".formatted(targetName)));
				})
				.orElseGet(() -> Result.err("Failed to parse source: " + parseResult.getProblems()));
	}

	private static TrimmedBounds toTrimmedBounds(LineIndexer lineIndexer, Position startPos, Position endPos) {
		final int start = lineIndexer.getIndex(startPos);
		int end = lineIndexer.getIndex(endPos);
		while (Character.isWhitespace(lineIndexer.getString().charAt(end - 1))) {
			end--;
		}

		return new TrimmedBounds(start, end);
	}

	private static ParseResult<CompilationUnit> parse(String source) {
		final ParserConfiguration config = new ParserConfiguration()
			.setStoreTokens(true)
			// .setSymbolResolver(new JavaSymbolSolver())
			.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

		final ParseResult<CompilationUnit> parseResult = new JavaParser(config).parse(source);
		return parseResult;
	}

	private Result<? extends Class<? extends TypeDeclaration<?>>, String> getNodeType(
			ClassEntry targetClass, String targetName
	) {
		final EntryIndex entryIndex = this.gui.getController().getProject().getJarIndex()
				.getIndex(EntryIndex.class);

		return Optional
			.ofNullable(entryIndex.getDefinition(targetClass))
			.map(targetDef -> {
				if (targetDef.getAccess().isAnnotation()) {
					return AnnotationDeclaration.class;
				} else if (targetDef.isEnum()) {
					return EnumDeclaration.class;
				} else if (targetDef.isRecord()) {
					return RecordDeclaration.class;
				} else {
					return ClassOrInterfaceDeclaration.class;
				}
			})
			.<Result<? extends Class<? extends TypeDeclaration<?>>, String>>map(Result::ok)
			.orElseGet(() -> Result.err(noDefinitionErrorOf(targetName)));
	}

	private static String noDefinitionErrorOf(String targetName) {
		return "No definition for %s!".formatted(targetName);
	}

	/**
	 * @see #consumeEditorMouseTarget(BiConsumer, Runnable)
	 */
	private void consumeEditorMouseTarget(BiConsumer<Token, Entry<?>> action) {
		this.consumeEditorMouseTarget(action, Runnables.doNothing());
	}

	/**
	 * If the mouse is currently over a {@link Token} in the {@link #editor} that resolves to an {@link Entry}, passes
	 * the token and entry to the passed {@code action}.<br>
	 * Otherwise, calls the passed {@code onNoTarget}.
	 *
	 * @param action     the action to run when the mouse is over a token that resolves to an entry
	 * @param onNoTarget the action to run when the mouse is not over a token that resolves to an entry
	 */
	private void consumeEditorMouseTarget(BiConsumer<Token, Entry<?>> action, Runnable onNoTarget) {
		consumeMousePositionIn(this.editor,
				(absoluteMouse, relativeMouse) -> Optional.of(relativeMouse)
					.map(this.editor::viewToModel2D)
					.filter(textPos -> textPos >= 0)
					.map(this::getToken)
					.ifPresentOrElse(
						token -> Optional.of(token)
							.map(this::getReference)
							.map(reference -> reference.entry)
							.ifPresentOrElse(
								entry -> action.accept(token, entry),
								onNoTarget
							),
						onNoTarget
					),
				ignored -> onNoTarget.run()
		);
	}

	/**
	 * @see #consumeMousePositionIn(Component, BiConsumer, Consumer)
	 */
	private static void consumeMousePositionIn(Component component, BiConsumer<Point, Point> inAction) {
		consumeMousePositionIn(component, inAction, pos -> { });
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
	private static void consumeMousePositionIn(
			Component component, BiConsumer<Point, Point> inAction, Consumer<Point> outAction
	) {
		final Point absolutePos = MouseInfo.getPointerInfo().getLocation();

		final Point componentPos = component.getLocationOnScreen();
		final Point relativePos = new Point(absolutePos);
		relativePos.translate(-componentPos.x, -componentPos.y);

		if (component.contains(relativePos)) {
			inAction.accept(absolutePos, relativePos);
		} else {
			outAction.accept(absolutePos);
		}
	}

	private TrimmedBounds createTrimmedBounds(DecompiledClassSource source, Entry<?> target, Entry<?> deobfTarget) {
		final String targetDotName = CLASS_PUNCTUATION.matcher(deobfTarget.getFullName()).replaceAll(".");

		if (target instanceof ClassEntry targetClass) {
			return unwrapOrNull(this.findClassBounds(source, targetClass, targetDotName));
		} else if (target instanceof MethodEntry targetMethod) {
			// TODO
			return unwrapOrNull(
					this.findMethodBounds(source, targetMethod, targetDotName, deobfTarget.getSimpleName())
			);
		} else if (target instanceof FieldEntry targetField) {
			// TODO
			return null;
		} else if (target instanceof LocalVariableEntry targetLocal) {
			if (targetLocal.isArgument()) {
				// TODO
				return null;
			} else {
				// TODO
				return null;

				// nothing? or show parent method?
			}
		} else {
			// TODO
			return null;
		}
	}

	private static TrimmedBounds unwrapOrNull(Result<TrimmedBounds, String> boundsResult) {
		return boundsResult.unwrapOrElse(error -> {
			Logger.error(error);
			return null;
		});
	}

	public void onRename(boolean isNewMapping) {
		this.navigatorPanel.updateAllTokenTypes();
		if (isNewMapping) {
			this.navigatorPanel.decrementIndex();
		}
	}

	@Override
	protected void initEditorPane(JPanel editorPane) {
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.anchor = GridBagConstraints.FIRST_LINE_END;
		constraints.insets = new Insets(32, 32, 32, 32);
		constraints.ipadx = 16;
		constraints.ipady = 16;
		editorPane.add(this.navigatorPanel, constraints);

		super.initEditorPane(editorPane);
	}

	@Nullable
	public static EditorPanel byUi(Component ui) {
		if (ui instanceof JComponent component) {
			Object prop = component.getClientProperty(EditorPanel.class);
			if (prop instanceof EditorPanel panel) {
				return panel;
			}
		}

		return null;
	}

	public NavigatorPanel getNavigatorPanel() {
		return this.navigatorPanel;
	}

	@Override
	protected void setClassHandleImpl(
			ClassEntry old, ClassHandle handle,
			@Nullable Function<DecompiledClassSource, TrimmedBounds> trimFactory
	) {
		super.setClassHandleImpl(old, handle, trimFactory);

		handle.addListener(new ClassHandleListener() {
			@Override
			public void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
				SwingUtilities.invokeLater(() -> EditorPanel.this.listeners.forEach(l -> l
					.onTitleChanged(EditorPanel.this, EditorPanel.this.getSimpleClassName()))
				);
			}

			@Override
			public void onDeleted(ClassHandle h) {
				SwingUtilities.invokeLater(() -> EditorPanel.this.gui.closeEditor(EditorPanel.this));
			}
		});

		this.listeners.forEach(l -> l.onClassHandleChanged(this, old, handle));
	}

	private void onCaretMove(int pos) {
		if (this.settingSource || this.controller.getProject() == null) {
			return;
		}

		this.setCursorReference(this.getReference(this.getToken(pos)));
	}

	private void navigateToCursorReference() {
		if (this.cursorReference != null) {
			final Entry<?> referenceEntry = this.cursorReference.entry;
			final Entry<?> navigationEntry = this.cursorReference.context == null
					? this.controller.getProject().getRemapper().getObfResolver()
						.resolveFirstEntry(referenceEntry, ResolutionStrategy.RESOLVE_ROOT)
					: referenceEntry;

			this.controller.navigateTo(navigationEntry);
		}
	}

	@Override
	protected void setCursorReference(EntryReference<Entry<?>, Entry<?>> ref) {
		super.setCursorReference(ref);

		this.popupMenu.updateUiState();

		this.listeners.forEach(l -> l.onCursorReferenceChanged(this, ref));
	}

	public void addListener(EditorActionListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(EditorActionListener listener) {
		this.listeners.remove(listener);
	}

	public void retranslateUi() {
		this.popupMenu.retranslateUi();
		this.quickFindToolBar.translate();
	}

	public void reloadKeyBinds() {
		putKeyBindAction(KeyBinds.EDITOR_RELOAD_CLASS, this.editor, e -> {
			if (this.classHandle != null) {
				this.classHandle.invalidate();
			}
		});
		putKeyBindAction(KeyBinds.EDITOR_ZOOM_IN, this.editor, e -> this.offsetEditorZoom(2));
		putKeyBindAction(KeyBinds.EDITOR_ZOOM_OUT, this.editor, e -> this.offsetEditorZoom(-2));
		putKeyBindAction(KeyBinds.EDITOR_QUICK_FIND, this.editor, new DefaultSyntaxAction("quick-find-tool-bar") {
			@Override
			public void actionPerformed(JTextComponent target, SyntaxDocument sDoc, int dot, ActionEvent e) {
				EditorPanel.this.quickFindToolBar.showFor(target);
			}
		});
		this.quickFindToolBar.reloadKeyBinds();

		this.popupMenu.getButtonKeyBinds().forEach((key, button) -> putKeyBindAction(key, this.editor, e -> button.doClick()));
	}
}

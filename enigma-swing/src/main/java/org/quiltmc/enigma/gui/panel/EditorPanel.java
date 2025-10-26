package org.quiltmc.enigma.gui.panel;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.TokenStore;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindToolBar;
import org.quiltmc.enigma.gui.element.EditorPopupMenu;
import org.quiltmc.enigma.gui.element.NavigatorPanel;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.syntaxpain.PairsMarker;
import org.tinylog.Logger;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import static org.quiltmc.enigma.gui.util.GuiUtil.consumeMousePositionIn;
import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;
import static javax.swing.SwingUtilities.isDescendingFrom;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public class EditorPanel extends AbstractEditorPanel<MarkableScrollPane> {
	private static final int DEOBFUSCATED_PRIORITY = 0;
	private static final int PROPOSED_PRIORITY = DEOBFUSCATED_PRIORITY + 1;
	private static final int FALLBACK_PRIORITY = PROPOSED_PRIORITY + 1;
	private static final int OBFUSCATED_PRIORITY = FALLBACK_PRIORITY + 1;
	private static final int DEBUG_PRIORITY = OBFUSCATED_PRIORITY + 1;

	private final NavigatorPanel navigatorPanel;
	private final EnigmaQuickFindToolBar quickFindToolBar = new EnigmaQuickFindToolBar();
	private final EditorPopupMenu popupMenu;

	private final TooltipManager tooltipManager = new TooltipManager();

	private final List<EditorActionListener> listeners = new ArrayList<>();

	public EditorPanel(Gui gui, NavigatorPanel navigator) {
		super(gui);

		this.navigatorPanel = navigator;

		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot()));

		PairsMarker.install(new PairsMarker(this.editor, Config.getCurrentSyntaxPaneColors().pairsMarker.value()));

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

		this.installEditorRuler(0);

		this.quickFindToolBar.setVisible(false);
		// init editor popup menu
		this.popupMenu = new EditorPopupMenu(this, gui);
		this.editor.setComponentPopupMenu(this.popupMenu.getUi());

		this.editor.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e1) {
				if ((e1.getModifiersEx() & CTRL_DOWN_MASK) != 0 && e1.getButton() == MouseEvent.BUTTON1) {
					// ctrl + left click
					EditorPanel.this.navigateToCursorReference();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e1) {
				switch (e1.getButton()) {
					case MouseEvent.BUTTON3 -> // Right click
							EditorPanel.this.editor.setCaretPosition(EditorPanel.this.editor.viewToModel2D(e1.getPoint()));
					case 4 -> // Back navigation
							gui.getController().openPreviousReference();
					case 5 -> // Forward navigation
							gui.getController().openNextReference();
				}
			}
		});

		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot()));

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

		this.popupMenu.getButtonKeyBinds().keySet().forEach(keyBind -> keyBind.removeUiConflicts(this.editor));

		this.reloadKeyBinds();
		this.addSourceSetListener(source -> {
			if (this.navigatorPanel != null) {
				this.navigatorPanel.resetEntries(source.getIndex().declarations());
			}

			this.editorScrollPane.clearMarkers();

			final SyntaxPaneProperties.Colors colors = Config.getCurrentSyntaxPaneColors();
			final TokenStore tokenStore = source.getTokenStore();
			tokenStore.getByType().forEach((type, tokens) -> {
				final Color nonFallbackColor;
				final int nonFallbackPriority;
				switch (type) {
					case OBFUSCATED -> {
						nonFallbackColor = colors.obfuscatedOutline.value();
						nonFallbackPriority = OBFUSCATED_PRIORITY;
					}
					case DEOBFUSCATED -> {
						nonFallbackColor = colors.deobfuscatedOutline.value();
						nonFallbackPriority = DEOBFUSCATED_PRIORITY;
					}
					case JAR_PROPOSED, DYNAMIC_PROPOSED -> {
						nonFallbackColor = colors.proposedOutline.value();
						nonFallbackPriority = PROPOSED_PRIORITY;
					}
					case DEBUG -> {
						nonFallbackColor = colors.debugTokenOutline.value();
						nonFallbackPriority = DEBUG_PRIORITY;
					}
					default -> throw new AssertionError();
				}

				for (final Token token : tokens) {
					final Color color;
					final int priority;
					if (tokenStore.isFallback(token)) {
						color = colors.fallbackOutline.value();
						priority = FALLBACK_PRIORITY;
					} else {
						color = nonFallbackColor;
						priority = nonFallbackPriority;
					}

					try {
						final int tokenPos = (int) this.editor.modelToView2D(token.start).getCenterY();

						this.editorScrollPane.addMarker(tokenPos, color, priority);
					} catch (BadLocationException e) {
						Logger.warn("Tried to add marker for token with bad location: " + token);
					}
				}
			});
		});

		this.ui.putClientProperty(EditorPanel.class, this);
	}

	@Override
	protected MarkableScrollPane createEditorScrollPane(JEditorPane editor) {
		return new MarkableScrollPane(editor);
	}

	public void onRename(boolean isNewMapping) {
		this.navigatorPanel.updateAllTokenTypes();
		if (isNewMapping) {
			this.navigatorPanel.decrementIndex();
		}
	}

	@Override
	protected void initEditorPane(JPanel editorPane) {
		editorPane.add(this.navigatorPanel, GridBagConstraintsBuilder.create()
				.pos(0, 0)
				.weight(1, 1)
				.anchor(GridBagConstraints.FIRST_LINE_END)
				.insets(32)
				.padding(16)
				.build()
		);

		super.initEditorPane(editorPane);

		editorPane.add(this.quickFindToolBar, GridBagConstraintsBuilder.create()
				.pos(0, 1)
				.weightX(1)
				.anchor(GridBagConstraints.PAGE_END)
				.fill(GridBagConstraints.HORIZONTAL)
				.build()
		);
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

	@Override
	public void destroy() {
		super.destroy();
		this.tooltipManager.removeExternalListeners();
	}

	public NavigatorPanel getNavigatorPanel() {
		return this.navigatorPanel;
	}

	@Override
	protected CompletableFuture<?> setClassHandleImpl(
			ClassEntry old, ClassHandle handle,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		final CompletableFuture<?> superFuture = super.setClassHandleImpl(old, handle, snippetFactory);

		this.editorScrollPane.clearMarkers();

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

		return superFuture;
	}

	private void onCaretMove(int pos) {
		if (this.settingSource || this.controller.getProject() == null) {
			return;
		}

		this.setCursorReference(this.getReference(this.getToken(pos)));
	}

	private void navigateToCursorReference() {
		if (this.cursorReference != null) {
			this.controller.navigateTo(this.resolveReference(this.cursorReference));
		}
	}

	@Override
	protected void setCursorReference(EntryReference<Entry<?>, Entry<?>> ref) {
		super.setCursorReference(ref);

		this.popupMenu.updateUiState();

		this.listeners.forEach(l -> l.onCursorReferenceChanged(this, ref));
	}

	@Override
	public void offsetEditorZoom(int zoomAmount) {
		super.offsetEditorZoom(zoomAmount);
		this.tooltipManager.entryTooltip.setZoom(zoomAmount);
	}

	@Override
	public void resetEditorZoom() {
		super.resetEditorZoom();
		this.tooltipManager.entryTooltip.resetZoom();
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
			if (this.classHandler != null) {
				this.classHandler.getHandle().invalidate();
			}
		});
		putKeyBindAction(KeyBinds.EDITOR_ZOOM_IN, this.editor, e -> this.offsetEditorZoom(2));
		putKeyBindAction(KeyBinds.EDITOR_ZOOM_OUT, this.editor, e -> this.offsetEditorZoom(-2));
		putKeyBindAction(KeyBinds.EDITOR_QUICK_FIND, this.editor, new TextAction("quick-find-tool-bar") {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JTextComponent target = this.getTextComponent(e);
				if (e != null) {
					EditorPanel.this.quickFindToolBar.showFor(target);
				}
			}
		});
		this.quickFindToolBar.reloadKeyBinds();

		this.popupMenu.getButtonKeyBinds().forEach((key, button) -> putKeyBindAction(key, this.editor, e -> button.doClick()));
	}

	private class TooltipManager {
		static final int MOUSE_STOPPED_MOVING_DELAY = 100;

		// DIY tooltip because JToolTip can't be moved or resized
		final EntryTooltip entryTooltip = new EntryTooltip(EditorPanel.this.gui);

		final WindowAdapter guiFocusListener = new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				if (e.getOppositeWindow() != TooltipManager.this.entryTooltip) {
					TooltipManager.this.entryTooltip.close();
				}
			}
		};

		final AWTEventListener globalKeyListener = e -> {
			if (e.getID() == KeyEvent.KEY_TYPED || e.getID() == KeyEvent.KEY_PRESSED) {
				this.reset();
			}
		};

		// Avoid finding the mouse entry every mouse movement update.
		// This also reduces the chances of accidentally updating the tooltip with
		// a new entry's content as you move your mouse to the tooltip.
		final Timer mouseStoppedMovingTimer = new Timer(MOUSE_STOPPED_MOVING_DELAY, e -> {
			if (Config.editor().entryTooltips.enable.value()) {
				EditorPanel.this.consumeEditorMouseTarget(
						(token, entry, resolvedParent) -> {
							this.hideTimer.stop();
							if (this.entryTooltip.isVisible()) {
								this.showTimer.stop();

								if (!token.equals(this.lastMouseTargetToken)) {
									this.lastMouseTargetToken = token;
									this.openTooltip(entry, resolvedParent);
								}
							} else {
								this.lastMouseTargetToken = token;
								this.showTimer.start();
							}
						},
						() -> consumeMousePositionIn(
							this.entryTooltip.getContentPane(),
							(absolute, relative) -> this.hideTimer.stop(),
							absolute -> {
								this.lastMouseTargetToken = null;
								this.showTimer.stop();
								this.hideTimer.start();
							}
						)
				);
			}
		});

		final Timer showTimer = new Timer(
				ToolTipManager.sharedInstance().getInitialDelay() - MOUSE_STOPPED_MOVING_DELAY, e -> {
					EditorPanel.this.consumeEditorMouseTarget((token, entry, resolvedParent) -> {
						if (token.equals(this.lastMouseTargetToken)) {
							this.entryTooltip.setVisible(true);
							this.openTooltip(entry, resolvedParent);
						}
					});
				}
		);

		final Timer hideTimer = new Timer(
				ToolTipManager.sharedInstance().getDismissDelay() - MOUSE_STOPPED_MOVING_DELAY,
				e -> this.entryTooltip.close()
		);

		@Nullable
		Token lastMouseTargetToken;

		TooltipManager() {
			this.mouseStoppedMovingTimer.setRepeats(false);
			this.showTimer.setRepeats(false);
			this.hideTimer.setRepeats(false);

			this.entryTooltip.setVisible(false);

			this.entryTooltip.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					if (Config.editor().entryTooltips.interactable.value()) {
						TooltipManager.this.mouseStoppedMovingTimer.stop();
						TooltipManager.this.hideTimer.stop();
					}
				}
			});

			this.entryTooltip.addCloseListener(TooltipManager.this::reset);

			EditorPanel.this.editor.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					TooltipManager.this.reset();
				}
			});

			EditorPanel.this.editor.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent mouseEvent) {
					TooltipManager.this.entryTooltip.close();
				}
			});

			EditorPanel.this.editor.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					if (!TooltipManager.this.entryTooltip.hasRepopulated()) {
						TooltipManager.this.mouseStoppedMovingTimer.restart();
					}
				}
			});

			EditorPanel.this.editorScrollPane.getViewport().addChangeListener(e -> this.entryTooltip.close());

			this.addExternalListeners();
		}

		void reset() {
			this.lastMouseTargetToken = null;
			this.mouseStoppedMovingTimer.stop();
			this.showTimer.stop();
			this.hideTimer.stop();
		}

		void openTooltip(Entry<?> target, boolean inherited) {
			final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			final Component eventReceiver = focusOwner != null && isDescendingFrom(focusOwner, EditorPanel.this.gui.getFrame())
					? focusOwner : null;

			this.entryTooltip.open(target, inherited, eventReceiver);
		}

		void addExternalListeners() {
			EditorPanel.this.gui.getFrame().addWindowFocusListener(this.guiFocusListener);
			Toolkit.getDefaultToolkit().addAWTEventListener(this.globalKeyListener, KeyEvent.KEY_EVENT_MASK);
		}

		void removeExternalListeners() {
			EditorPanel.this.gui.getFrame().removeWindowFocusListener(this.guiFocusListener);
			Toolkit.getDefaultToolkit().removeAWTEventListener(this.globalKeyListener);
		}
	}
}

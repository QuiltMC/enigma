package org.quiltmc.enigma.gui.panel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import com.google.common.collect.ImmutableMap;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.TokenStore;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.EntryMarkersSection;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindToolBar;
import org.quiltmc.enigma.gui.element.EditorPopupMenu;
import org.quiltmc.enigma.gui.element.NavigatorPanel;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.syntaxpain.PairsMarker;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.LineIndexer;
import org.tinylog.Logger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
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
	private final NavigatorPanel navigatorPanel;
	private final EnigmaQuickFindToolBar quickFindToolBar = new EnigmaQuickFindToolBar();
	private final EditorPopupMenu popupMenu;

	private final EntryTooltipManager entryTooltipManager = new EntryTooltipManager();

	private final List<EditorActionListener> listeners = new ArrayList<>();

	@NonNull
	private MarkerManager markerManager = this.createMarkerManager();

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
				if (!e1.isConsumed() && (e1.getModifiersEx() & CTRL_DOWN_MASK) != 0 && e1.getButton() == MouseEvent.BUTTON1) {
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

			this.refreshMarkers(source);
		});

		this.ui.putClientProperty(EditorPanel.class, this);

		final EntryMarkersSection markersConfig = Config.editor().entryMarkers;
		this.registerMarkerRefresher(markersConfig.onlyMarkDeclarations, MarkerManager::onlyMarksDeclarations);
		this.registerMarkerRefresher(markersConfig.markObfuscated, MarkerManager::marksObfuscated);
		this.registerMarkerRefresher(markersConfig.markFallback, MarkerManager::marksFallback);
		this.registerMarkerRefresher(markersConfig.markProposed, MarkerManager::marksProposed);
		this.registerMarkerRefresher(markersConfig.markDeobfuscated, MarkerManager::marksDeobfuscated);

		markersConfig.maxMarkersPerLine.registerCallback(updated -> {
			this.editorScrollPane.setMaxConcurrentMarkers(updated.value());
		});
	}

	private void registerMarkerRefresher(TrackedValue<Boolean> config, Predicate<MarkerManager> handlerGetter) {
		config.registerCallback(updated -> {
			if (updated.value() != handlerGetter.test(this.markerManager)) {
				this.markerManager = this.createMarkerManager();

				final DecompiledClassSource source = this.getSource();
				if (source != null) {
					this.refreshMarkers(source);
				} else {
					this.editorScrollPane.clearMarkers();
				}
			}
		});
	}

	private void refreshMarkers(DecompiledClassSource source) {
		this.editorScrollPane.clearMarkers();

		final TokenStore tokenStore = source.getTokenStore();
		tokenStore.getByType().forEach((type, tokens) -> {
			for (final Token token : tokens) {
				this.markerManager.tryMarking(token, type, tokenStore);
			}
		});
	}

	@Override
	protected MarkableScrollPane createEditorScrollPane(JEditorPane editor) {
		return new MarkableScrollPane(editor, Config.editor().entryMarkers.maxMarkersPerLine.value());
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
		this.entryTooltipManager.removeExternalListeners();
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
		this.entryTooltipManager.tooltip.setZoom(zoomAmount);
	}

	@Override
	public void resetEditorZoom() {
		super.resetEditorZoom();
		this.entryTooltipManager.tooltip.resetZoom();
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

		this.popupMenu.getButtonKeyBinds()
				.forEach((key, button) -> putKeyBindAction(key, this.editor, e -> button.doClick(0)));
	}

	private MarkerManager createMarkerManager() {
		final EntryMarkersSection markersConfig = Config.editor().entryMarkers;
		return new MarkerManager(
			markersConfig.onlyMarkDeclarations.value(),
			markersConfig.markObfuscated.value(),
			markersConfig.markFallback.value(),
			markersConfig.markProposed.value(),
			markersConfig.markDeobfuscated.value()
		);
	}

	private class EntryTooltipManager {
		static final int MOUSE_STOPPED_MOVING_DELAY = 100;

		final EntryTooltip tooltip = new EntryTooltip(EditorPanel.this.gui);

		final WindowAdapter guiFocusListener = new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				if (e.getOppositeWindow() != EntryTooltipManager.this.tooltip) {
					EntryTooltipManager.this.tooltip.close();
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
			if (
					Config.editor().entryTooltips.enable.value()
						&& !EditorPanel.this.markerManager.markerTooltip.isShowing()
			) {
				EditorPanel.this.consumeEditorMouseTarget(
						(token, entry, resolvedParent) -> {
							this.hideTimer.stop();
							if (this.tooltip.isVisible()) {
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
							this.tooltip.getContentPane(),
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
							this.tooltip.setVisible(true);
							this.openTooltip(entry, resolvedParent);
						}
					});
				}
		);

		final Timer hideTimer = new Timer(
				ToolTipManager.sharedInstance().getDismissDelay() - MOUSE_STOPPED_MOVING_DELAY,
				e -> this.tooltip.close()
		);

		@Nullable
		Token lastMouseTargetToken;

		EntryTooltipManager() {
			this.mouseStoppedMovingTimer.setRepeats(false);
			this.showTimer.setRepeats(false);
			this.hideTimer.setRepeats(false);

			this.tooltip.setVisible(false);

			this.tooltip.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					if (Config.editor().entryTooltips.interactable.value()) {
						EntryTooltipManager.this.mouseStoppedMovingTimer.stop();
						EntryTooltipManager.this.hideTimer.stop();
					}
				}
			});

			this.tooltip.addCloseListener(EntryTooltipManager.this::reset);

			EditorPanel.this.editor.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					EntryTooltipManager.this.reset();
				}
			});

			EditorPanel.this.editor.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent mouseEvent) {
					EntryTooltipManager.this.tooltip.close();
				}
			});

			EditorPanel.this.editor.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					if (!EntryTooltipManager.this.tooltip.hasRepopulated()) {
						EntryTooltipManager.this.mouseStoppedMovingTimer.restart();
					}
				}
			});

			EditorPanel.this.editorScrollPane.getViewport().addChangeListener(e -> this.tooltip.close());

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
			final Component eventReceiver =
					focusOwner != null && isDescendingFrom(focusOwner, EditorPanel.this.gui.getFrame())
						? focusOwner : null;

			this.tooltip.open(target, inherited, eventReceiver);
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

	private class MarkerManager {
		static final ImmutableMap<TrackedValue<ThemeProperties.SerializableColor>, Integer>
				MARKER_PRIORITIES_BY_COLOR_CONFIG;

		static {
			int priority = 0;
			MARKER_PRIORITIES_BY_COLOR_CONFIG = ImmutableMap.of(
				Config.getCurrentSyntaxPaneColors().deobfuscatedOutline, priority++,
				Config.getCurrentSyntaxPaneColors().proposedOutline, priority++,
				Config.getCurrentSyntaxPaneColors().fallbackOutline, priority++,
				Config.getCurrentSyntaxPaneColors().obfuscatedOutline, priority++,
				Config.getCurrentSyntaxPaneColors().debugTokenOutline, priority
			);
		}

		final MarkerTooltip markerTooltip = new MarkerTooltip();

		final boolean onlyMarkDeclarations;

		final boolean markObfuscated;
		final boolean markFallback;
		final boolean markProposed;
		final boolean markDeobfuscated;

		MarkerManager(
				boolean onlyMarkDeclarations,
				boolean markObfuscated, boolean markFallback, boolean markProposed, boolean markDeobfuscated
		) {
			this.onlyMarkDeclarations = onlyMarkDeclarations;
			this.markObfuscated = markObfuscated;
			this.markFallback = markFallback;
			this.markProposed = markProposed;
			this.markDeobfuscated = markDeobfuscated;
		}

		void tryMarking(Token token, TokenType type, TokenStore tokenStore) {
			if (this.onlyMarkDeclarations) {
				final EntryReference<Entry<?>, Entry<?>> reference =
						EditorPanel.this.getReference(token);

				if (reference != null) {
					if (reference.entry instanceof MethodEntry method && method.isConstructor()) {
						return;
					}

					final Entry<?> resolved = EditorPanel.this.resolveReference(reference);
					final EntryReference<Entry<?>, Entry<?>> declaration = EntryReference
							.declaration(resolved, resolved.getName());

					if (
							EditorPanel.this.getReferences(declaration).stream()
								.findFirst()
								.filter(declarationToken -> !declarationToken.equals(token))
								.isPresent()
					) {
						return;
					}
				}
			}

			final TrackedValue<ThemeProperties.SerializableColor> colorConfig =
					this.getColorConfig(token, type, tokenStore);

			if (colorConfig != null) {
				try {
					final int tokenPos = (int) EditorPanel.this.editor.modelToView2D(token.start).getCenterY();

					final int priority = Objects.requireNonNull(MARKER_PRIORITIES_BY_COLOR_CONFIG.get(colorConfig));
					final Color color = colorConfig.value();
					EditorPanel.this.editorScrollPane.addMarker(
							tokenPos, color, priority,
							new MarkerListener(token)
					);
				} catch (BadLocationException e) {
					Logger.warn("Tried to add marker for token with bad location: " + token);
				}
			}
		}

		@Nullable
		private TrackedValue<ThemeProperties.SerializableColor> getColorConfig(
				Token token, TokenType type, TokenStore tokenStore
		) {
			if (tokenStore.isFallback(token)) {
				return this.markFallback ? Config.getCurrentSyntaxPaneColors().fallbackOutline : null;
			} else {
				return switch (type) {
					case OBFUSCATED -> this.markObfuscated
							? Config.getCurrentSyntaxPaneColors().obfuscatedOutline
							: null;
					case DEOBFUSCATED -> this.markDeobfuscated
							? Config.getCurrentSyntaxPaneColors().deobfuscatedOutline
							: null;
					case JAR_PROPOSED, DYNAMIC_PROPOSED -> this.markProposed
							? Config.getCurrentSyntaxPaneColors().proposedOutline
							: null;
					// these only appear if debugTokenHighlights is true, so no need for a separate marker config
					case DEBUG -> Config.getCurrentSyntaxPaneColors().debugTokenOutline;
				};
			}
		}

		boolean onlyMarksDeclarations() {
			return this.onlyMarkDeclarations;
		}

		boolean marksObfuscated() {
			return this.markObfuscated;
		}

		boolean marksFallback() {
			return this.markFallback;
		}

		boolean marksProposed() {
			return this.markProposed;
		}

		boolean marksDeobfuscated() {
			return this.markDeobfuscated;
		}

		private class MarkerListener implements MarkableScrollPane.MarkerListener {
			private final Token token;

			MarkerListener(Token token) {
				this.token = token;
			}

			@Override
			public void mouseClicked(int x, int y) {
				EditorPanel.this.navigateToToken(this.token);
			}

			@Override
			public void mouseExited(int x, int y) {
				if (
						Config.editor().entryMarkers.tooltip.value()
							&& EditorPanel.this.entryTooltipManager.lastMouseTargetToken == null
				) {
					MarkerManager.this.markerTooltip.close();
				}
			}

			@Override
			public void mouseEntered(int x, int y) {
				if (Config.editor().entryMarkers.tooltip.value()) {
					EditorPanel.this.entryTooltipManager.tooltip.close();
					MarkerManager.this.markerTooltip.open(this.token, x, y);
				}
			}

			@Override
			public void mouseTransferred(int x, int y) {
				this.mouseEntered(x, y);
			}

			@Override
			public void mouseMoved(int x, int y) {
				if (Config.editor().entryMarkers.tooltip.value()) {
					EditorPanel.this.entryTooltipManager.tooltip.close();
				}
			}
		}
	}

	private class MarkerTooltip extends JWindow {
		public static final int DEFAULT_MARKER_PAD = 5;
		final JPanel content = new JPanel();

		// HACK to make getPreferredSize aware of its (future) position
		// negative values indicate it's un-set and should be ignored
		int right = -1;

		MarkerTooltip() {
			this.setContentPane(this.content);

			this.setAlwaysOnTop(true);
			this.setType(Window.Type.POPUP);
			this.setLayout(new BorderLayout());
		}

		void open(Token target, int markerX, int markerY) {
			this.content.removeAll();

			if (EditorPanel.this.classHandler == null) {
				return;
			}

			final SimpleSnippetPanel snippet = new SimpleSnippetPanel(EditorPanel.this.gui, target);

			this.content.add(snippet.ui);

			snippet.setSource(EditorPanel.this.getSource(), source -> {
				final String sourceString = source.toString();
				final LineIndexer lineIndexer = source.getLineIndexer();
				final int line = lineIndexer.getLine(target.start);
				int lineStart = lineIndexer.getStartIndex(line);
				int lineEnd = lineIndexer.getStartIndex(line + 1);

				if (lineEnd < 0) {
					lineEnd = sourceString.length();
				}

				while (lineStart < lineEnd && Character.isWhitespace(sourceString.charAt(lineStart))) {
					lineStart++;
				}

				while (lineEnd > lineStart && Character.isWhitespace(sourceString.charAt(lineEnd - 1))) {
					lineEnd--;
				}

				return new Snippet(lineStart, lineEnd);
			});

			this.right = markerX - ScaleUtil.scale(DEFAULT_MARKER_PAD);

			this.pack();

			this.setLocation(this.right - this.getWidth(), markerY - this.getHeight() / 2);

			this.right = -1;

			this.setVisible(true);
		}

		void close() {
			this.setVisible(false);
			this.content.removeAll();
		}

		@Override
		public Dimension getPreferredSize() {
			final Dimension size = super.getPreferredSize();

			if (this.right >= 0) {
				final int left = this.right - size.width;
				if (left < 0) {
					// don't extend off the left side of the screen
					size.width += left;
				}
			}

			return size;
		}
	}
}

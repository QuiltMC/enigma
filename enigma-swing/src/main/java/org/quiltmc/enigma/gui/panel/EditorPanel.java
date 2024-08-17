package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.class_handle.ClassHandleError;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.gui.BrowserCaret;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.element.EditorPopupMenu;
import org.quiltmc.enigma.gui.element.NavigatorPanel;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.gui.highlight.SelectionHighlightPainter;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Result;
import org.tinylog.Logger;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter.HighlightPainter;

public class EditorPanel {
	private final JPanel ui = new JPanel();
	private final JEditorPane editor = new JEditorPane();
	private final JScrollPane editorScrollPane = new JScrollPane(this.editor);
	private final EditorPopupMenu popupMenu;

	// progress UI
	private final JLabel decompilingLabel = new JLabel(I18n.translate("editor.decompiling"), SwingConstants.CENTER);
	private final JProgressBar decompilingProgressBar = new JProgressBar(0, 100);

	// error display UI
	private final JLabel errorLabel = new JLabel();
	private final JTextArea errorTextArea = new JTextArea();
	private final JScrollPane errorScrollPane = new JScrollPane(this.errorTextArea);
	private final JButton retryButton = new JButton(I18n.translate("prompt.retry"));
	private final NavigatorPanel navigatorPanel;

	private DisplayMode mode = DisplayMode.INACTIVE;

	private final GuiController controller;
	private final Gui gui;

	private EntryReference<Entry<?>, Entry<?>> cursorReference;
	private EntryReference<Entry<?>, Entry<?>> nextReference;
	private boolean mouseIsPressed = false;
	private boolean shouldNavigateOnClick;

	private int fontSize = 12;
	private final Map<TokenType, BoxHighlightPainter> boxHighlightPainters;

	private final List<EditorActionListener> listeners = new ArrayList<>();

	private ClassHandle classHandle;
	private DecompiledClassSource source;
	private boolean settingSource;

	public EditorPanel(Gui gui, NavigatorPanel navigator) {
		this.gui = gui;
		this.controller = gui.getController();
		this.navigatorPanel = navigator;

		this.editor.setEditable(false);
		this.editor.setLayout(new FlowLayout(FlowLayout.RIGHT));
		this.editor.setSelectionColor(new Color(31, 46, 90));
		this.editor.setCaret(new BrowserCaret());
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot(), this.mouseIsPressed));
		this.editor.setCaretColor(Config.getCurrentSyntaxPaneColors().caret.value());
		this.editor.setContentType("text/enigma-sources");
		this.editor.setBackground(Config.getCurrentSyntaxPaneColors().editorBackground.value());

		// set unit increment to height of one line, the amount scrolled per
		// mouse wheel rotation is then controlled by OS settings
		this.editorScrollPane.getVerticalScrollBar().setUnitIncrement(this.editor.getFontMetrics(this.editor.getFont()).getHeight());

		// init editor popup menu
		this.popupMenu = new EditorPopupMenu(this, gui);
		this.editor.setComponentPopupMenu(this.popupMenu.getUi());

		this.decompilingLabel.setFont(ScaleUtil.getFont(this.decompilingLabel.getFont().getFontName(), Font.BOLD, 26));
		this.decompilingProgressBar.setIndeterminate(true);
		this.errorTextArea.setEditable(false);
		this.errorTextArea.setFont(ScaleUtil.getFont(Font.MONOSPACED, Font.PLAIN, 10));

		this.boxHighlightPainters = ThemeUtil.getBoxHighlightPainters();

		this.editor.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				EditorPanel.this.mouseIsPressed = true;
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

				EditorPanel.this.mouseIsPressed = false;
			}
		});
		this.editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (EditorPanel.this.popupMenu.handleKeyEvent(event)) return;

				if (KeyBinds.EDITOR_RELOAD_CLASS.matches(event)) {
					if (EditorPanel.this.classHandle != null) {
						EditorPanel.this.classHandle.invalidate();
					}
				} else if (KeyBinds.EDITOR_QUICK_FIND.matches(event)) {
					// prevent navigating on click when quick find activated
				} else if (KeyBinds.EDITOR_ZOOM_IN.matches(event)) {
					EditorPanel.this.offsetEditorZoom(2);
				} else if (KeyBinds.EDITOR_ZOOM_OUT.matches(event)) {
					EditorPanel.this.offsetEditorZoom(-2);
				} else if (event.isControlDown()) {
					EditorPanel.this.shouldNavigateOnClick = true; // CTRL
				}
			}

			@Override
			public void keyTyped(KeyEvent event) {
				EntryReference<Entry<?>, Entry<?>> ref = EditorPanel.this.getCursorReference();
				if (ref == null) return;
				if (!EditorPanel.this.controller.getProject().isRenamable(ref)) return;

				if (!event.isControlDown() && !event.isAltDown() && Character.isJavaIdentifierPart(event.getKeyChar())) {
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

			@Override
			public void keyReleased(KeyEvent event) {
				EditorPanel.this.shouldNavigateOnClick = event.isControlDown();
			}
		});

		this.retryButton.addActionListener(e -> this.redecompileClass());

		this.ui.putClientProperty(EditorPanel.class, this);
	}

	public void onRename() {
		this.navigatorPanel.updateAllTokenTypes();
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

	public void setClassHandle(ClassHandle handle) {
		ClassEntry old = null;
		if (this.classHandle != null) {
			old = this.classHandle.getRef();
			this.classHandle.close();
		}

		this.setClassHandle0(old, handle);
	}

	private void setClassHandle0(ClassEntry old, ClassHandle handle) {
		this.setDisplayMode(DisplayMode.IN_PROGRESS);
		this.setCursorReference(null);

		handle.addListener(new ClassHandleListener() {
			@Override
			public void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
				SwingUtilities.invokeLater(() -> EditorPanel.this.listeners.forEach(l -> l.onTitleChanged(EditorPanel.this, EditorPanel.this.getFileName())));
			}

			@Override
			public void onMappedSourceChanged(ClassHandle h, Result<DecompiledClassSource, ClassHandleError> res) {
				EditorPanel.this.handleDecompilerResult(res);
			}

			@Override
			public void onInvalidate(ClassHandle h, InvalidationType t) {
				SwingUtilities.invokeLater(() -> {
					if (t == InvalidationType.FULL) {
						EditorPanel.this.setDisplayMode(DisplayMode.IN_PROGRESS);
					}
				});
			}

			@Override
			public void onDeleted(ClassHandle h) {
				SwingUtilities.invokeLater(() -> EditorPanel.this.gui.closeEditor(EditorPanel.this));
			}
		});

		handle.getSource().thenAcceptAsync(this::handleDecompilerResult, SwingUtilities::invokeLater);

		this.classHandle = handle;
		this.listeners.forEach(l -> l.onClassHandleChanged(this, old, handle));
	}

	public void destroy() {
		this.classHandle.close();
	}

	private void redecompileClass() {
		if (this.classHandle != null) {
			this.classHandle.invalidate();
		}
	}

	private void handleDecompilerResult(Result<DecompiledClassSource, ClassHandleError> res) {
		SwingUtilities.invokeLater(() -> {
			if (res.isOk()) {
				this.setSource(res.unwrap());
			} else {
				this.displayError(res.unwrapErr());
			}

			this.nextReference = null;
		});
	}

	public void displayError(ClassHandleError t) {
		this.setDisplayMode(DisplayMode.ERRORED);
		String str = switch (t.type) {
			case DECOMPILE -> "editor.decompile_error";
			case REMAP -> "editor.remap_error";
		};
		this.errorLabel.setText(I18n.translate(str));
		this.errorTextArea.setText(t.getStackTrace());
		this.errorTextArea.setCaretPosition(0);
	}

	public void setDisplayMode(DisplayMode mode) {
		if (this.mode == mode) return;
		this.ui.removeAll();
		switch (mode) {
			case INACTIVE:
				break;
			case IN_PROGRESS: {
				// make progress bar start from the left every time
				this.decompilingProgressBar.setIndeterminate(false);
				this.decompilingProgressBar.setIndeterminate(true);

				this.ui.setLayout(new GridBagLayout());
				GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);
				this.ui.add(this.decompilingLabel, cb.pos(0, 0).anchor(GridBagConstraints.SOUTH).build());
				this.ui.add(this.decompilingProgressBar, cb.pos(0, 1).anchor(GridBagConstraints.NORTH).build());
				break;
			}
			case SUCCESS: {
				this.ui.setLayout(new GridLayout(1, 1, 0, 0));

				JPanel editorPane = new JPanel() {
					@Override
					public boolean isOptimizedDrawingEnabled() {
						return false;
					}
				};
				editorPane.setLayout(new GridBagLayout());

				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.weightx = 1.0;
				constraints.weighty = 1.0;
				constraints.anchor = GridBagConstraints.FIRST_LINE_END;
				constraints.insets = new Insets(32, 32, 32, 32);
				constraints.ipadx = 16;
				constraints.ipady = 16;
				editorPane.add(this.navigatorPanel, constraints);

				constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.weightx = 1.0;
				constraints.weighty = 1.0;
				constraints.fill = GridBagConstraints.BOTH;
				editorPane.add(this.editorScrollPane, constraints);
				this.ui.add(editorPane);
				break;
			}
			case ERRORED: {
				this.ui.setLayout(new GridBagLayout());
				GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2).weight(1.0, 0.0).anchor(GridBagConstraints.WEST);
				this.ui.add(this.errorLabel, cb.pos(0, 0).build());
				this.ui.add(new JSeparator(SwingConstants.HORIZONTAL), cb.pos(0, 1).fill(GridBagConstraints.HORIZONTAL).build());
				this.ui.add(this.errorScrollPane, cb.pos(0, 2).weight(1.0, 1.0).fill(GridBagConstraints.BOTH).build());
				this.ui.add(this.retryButton, cb.pos(0, 3).weight(0.0, 0.0).anchor(GridBagConstraints.EAST).build());
				break;
			}
		}

		this.ui.validate();
		this.ui.repaint();
		this.mode = mode;
	}

	public void offsetEditorZoom(int zoomAmount) {
		int newResult = this.fontSize + zoomAmount;
		if (newResult > 8 && newResult < 72) {
			this.fontSize = newResult;
			this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		}
	}

	public void resetEditorZoom() {
		this.fontSize = 12;
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
	}

	public void onCaretMove(int pos, boolean fromClick) {
		if (this.settingSource || this.controller.getProject() == null) {
			return;
		}

		EntryRemapper mapper = this.controller.getProject().getRemapper();
		Token token = this.getToken(pos);

		this.setCursorReference(this.getReference(token));

		Entry<?> referenceEntry = this.cursorReference != null ? this.cursorReference.entry : null;

		if (referenceEntry != null && this.shouldNavigateOnClick && fromClick) {
			this.shouldNavigateOnClick = false;
			Entry<?> navigationEntry = referenceEntry;
			if (this.cursorReference.context == null) {
				EntryResolver resolver = mapper.getObfResolver();
				navigationEntry = resolver.resolveFirstEntry(referenceEntry, ResolutionStrategy.RESOLVE_ROOT);
			}

			this.controller.navigateTo(navigationEntry);
		}
	}

	private void setCursorReference(EntryReference<Entry<?>, Entry<?>> ref) {
		this.cursorReference = ref;

		this.popupMenu.updateUiState();

		this.listeners.forEach(l -> l.onCursorReferenceChanged(this, ref));
	}

	public Token getToken(int pos) {
		if (this.source == null) {
			return null;
		}

		return this.source.getIndex().getReferenceToken(pos);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (this.source == null) {
			return null;
		}

		return this.source.getIndex().getReference(token);
	}

	public void setSource(DecompiledClassSource source) {
		this.setDisplayMode(DisplayMode.SUCCESS);
		if (source == null) return;
		try {
			this.settingSource = true;

			int newCaretPos = 0;
			if (this.source != null && this.source.getEntry().equals(source.getEntry())) {
				int caretPos = this.editor.getCaretPosition();

				if (this.source.getTokenStore().isCompatible(source.getTokenStore())) {
					newCaretPos = this.source.getTokenStore().mapPosition(source.getTokenStore(), caretPos);
				} else {
					// if the class is the same but the token stores aren't
					// compatible, then the user probably switched decompilers

					// check if there's a selected reference we can navigate to,
					// but only if there's none already queued up for being selected
					if (this.getCursorReference() != null && this.nextReference == null) {
						this.nextReference = this.getCursorReference();
					}

					// otherwise fall back to just using the same average
					// position in the file
					float scale = (float) source.toString().length() / this.source.toString().length();
					newCaretPos = (int) (caretPos * scale);
				}
			}

			this.source = source;
			this.editor.getHighlighter().removeAllHighlights();
			this.editor.setText(source.toString());

			this.setHighlightedTokens(source.getHighlightedTokens());
			if (this.source != null) {
				this.editor.setCaretPosition(newCaretPos);

				for (Entry<?> entry : this.source.getIndex().declarations()) {
					this.navigatorPanel.addEntry(entry);
				}
			}

			this.setCursorReference(this.getReference(this.getToken(this.editor.getCaretPosition())));
		} finally {
			this.settingSource = false;
		}

		if (this.nextReference != null) {
			this.showReference0(this.nextReference);
			this.nextReference = null;
		}
	}

	public void setHighlightedTokens(Map<TokenType, ? extends Collection<Token>> tokens) {
		// remove any old highlighters
		this.editor.getHighlighter().removeAllHighlights();

		if (this.boxHighlightPainters != null) {
			BoxHighlightPainter proposedPainter = this.boxHighlightPainters.get(TokenType.JAR_PROPOSED);

			for (TokenType type : tokens.keySet()) {
				BoxHighlightPainter painter = this.boxHighlightPainters.get(type);

				if (painter != null) {
					for (Token token : tokens.get(type)) {
						EntryReference<Entry<?>, Entry<?>> reference = this.getReference(token);
						BoxHighlightPainter tokenPainter;

						if (reference != null) {
							EditableType t = EditableType.fromEntry(reference.entry);
							boolean editable = t == null || this.gui.isEditable(t);
							tokenPainter = editable ? painter : proposedPainter;
						} else {
							tokenPainter = painter;
						}

						this.addHighlightedToken(token, tokenPainter);
					}
				}
			}
		}

		this.editor.validate();
		this.editor.repaint();
	}

	private void addHighlightedToken(Token token, HighlightPainter tokenPainter) {
		try {
			this.editor.getHighlighter().addHighlight(token.start, token.end, tokenPainter);
		} catch (BadLocationException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		return this.cursorReference;
	}

	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (this.mode == DisplayMode.SUCCESS) {
			this.showReference0(reference);
		} else if (this.mode != DisplayMode.ERRORED) {
			this.nextReference = reference;
		}
	}

	/**
	 * Navigates to the reference without modifying history. Assumes the class is loaded.
	 */
	private void showReference0(EntryReference<Entry<?>, Entry<?>> reference) {
		if (this.source == null || reference == null) {
			return;
		}

		List<Token> tokens = this.controller.getTokensForReference(this.source, reference);
		if (tokens.isEmpty()) {
			// DEBUG
			Logger.debug("No tokens found for {} in {}", reference, this.classHandle.getRef());
		} else {
			this.gui.showTokens(this, tokens);
		}
	}

	public void navigateToToken(Token token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null!");
		}

		this.navigateToToken(token, SelectionHighlightPainter.INSTANCE);
	}

	private void navigateToToken(Token token, HighlightPainter highlightPainter) {
		// set the caret position to the token
		Document document = this.editor.getDocument();
		int clampedPosition = Math.min(Math.max(token.start, 0), document.getLength());

		this.editor.setCaretPosition(clampedPosition);
		this.editor.grabFocus();

		try {
			// make sure the token is visible in the scroll window
			Rectangle2D start = this.editor.modelToView2D(token.start);
			Rectangle2D end = this.editor.modelToView2D(token.end);
			if (start == null || end == null) {
				return;
			}

			Rectangle show = new Rectangle();
			Rectangle2D.union(start, end, show);
			show.grow((int) (start.getWidth() * 10), (int) (start.getHeight() * 6));
			SwingUtilities.invokeLater(() -> this.editor.scrollRectToVisible(show));
		} catch (BadLocationException ex) {
			if (!this.settingSource) {
				throw new RuntimeException(ex);
			} else {
				return;
			}
		}

		// highlight the token momentarily
		Timer timer = new Timer(200, new ActionListener() {
			private int counter = 0;
			private Object highlight = null;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (this.counter % 2 == 0) {
					try {
						this.highlight = EditorPanel.this.editor.getHighlighter().addHighlight(token.start, token.end, highlightPainter);
					} catch (BadLocationException ex) {
						// don't care
					}
				} else if (this.highlight != null) {
					EditorPanel.this.editor.getHighlighter().removeHighlight(this.highlight);
				}

				if (this.counter++ > 6) {
					Timer timer = (Timer) event.getSource();
					timer.stop();
				}
			}
		});

		timer.start();
	}

	public void addListener(EditorActionListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(EditorActionListener listener) {
		this.listeners.remove(listener);
	}

	public JPanel getUi() {
		return this.ui;
	}

	public JEditorPane getEditor() {
		return this.editor;
	}

	public DecompiledClassSource getSource() {
		return this.source;
	}

	public ClassHandle getClassHandle() {
		return this.classHandle;
	}

	public String getFileName() {
		ClassEntry classEntry = this.classHandle.getDeobfRef() != null ? this.classHandle.getDeobfRef() : this.classHandle.getRef();
		return classEntry.getSimpleName();
	}

	public void retranslateUi() {
		this.popupMenu.retranslateUi();
	}

	public void reloadKeyBinds() {
		this.popupMenu.setKeyBinds();
	}

	private enum DisplayMode {
		INACTIVE,
		IN_PROGRESS,
		SUCCESS,
		ERRORED,
	}
}

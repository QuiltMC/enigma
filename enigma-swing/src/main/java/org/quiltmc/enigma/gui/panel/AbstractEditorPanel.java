package org.quiltmc.enigma.gui.panel;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Runnables;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.class_handle.ClassHandleError;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.source.TokenStore;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.BrowserCaret;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.SelectionHighlightSection;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.LineIndexer;
import org.quiltmc.enigma.util.Result;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.syntaxpain.JavaSyntaxKit;
import org.quiltmc.syntaxpain.LineNumbersRuler;
import org.tinylog.Logger;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static org.quiltmc.enigma.gui.util.GuiUtil.consumeMousePositionIn;
import static org.quiltmc.enigma.gui.util.GuiUtil.getRecordIndexingService;

public abstract class AbstractEditorPanel<S extends JScrollPane> {
	protected final JPanel ui = new JPanel();
	protected final JEditorPane editor = new JEditorPane();
	protected final S editorScrollPane = this.createEditorScrollPane(this.editor);

	protected final GuiController controller;
	protected final Gui gui;

	// progress UI
	private final JLabel decompilingLabel = new JLabel(I18n.translate("editor.decompiling"), SwingConstants.CENTER);
	private final JProgressBar decompilingProgressBar = new JProgressBar(0, 100);

	// error display UI
	private final JLabel errorLabel = new JLabel();
	private final JTextArea errorTextArea = new JTextArea();
	private final JScrollPane errorScrollPane = new JScrollPane(this.errorTextArea);
	private final JButton retryButton = new JButton(I18n.translate("prompt.retry"));

	private final List<Consumer<DecompiledClassSource>> sourceSetListeners = new ArrayList<>();

	private DisplayMode mode = DisplayMode.INACTIVE;

	protected EntryReference<Entry<?>, Entry<?>> cursorReference;
	private EntryReference<Entry<?>, Entry<?>> nextReference;

	private int fontSize = 12;
	private final BoxHighlightPainter obfuscatedPainter;
	private final BoxHighlightPainter proposedPainter;
	private final BoxHighlightPainter deobfuscatedPainter;
	private final BoxHighlightPainter debugPainter;
	private final BoxHighlightPainter fallbackPainter;

	// package-private so EditorHighlightHandler can clean up after itself in finish()
	@Nullable
	EditorHighlightHandler selectionHighlightHandler;

	protected ClassHandler classHandler;
	private DecompiledClassSource source;
	private SourceBounds sourceBounds = new DefaultBounds();
	protected boolean settingSource;

	public AbstractEditorPanel(Gui gui) {
		this.gui = gui;
		this.controller = gui.getController();

		final SyntaxPaneProperties.Colors syntaxColors = Config.getCurrentSyntaxPaneColors();

		this.editor.setEditable(false);
		this.editor.setLayout(new FlowLayout(FlowLayout.RIGHT));
		this.editor.setSelectionColor(syntaxColors.selection.value());
		this.editor.setCaret(new BrowserCaret());
		this.editor.setFont(ScaleUtil.getFont(this.editor.getFont().getFontName(), Font.PLAIN, this.fontSize));
		this.editor.setCaretColor(syntaxColors.caret.value());
		this.editor.setContentType(JavaSyntaxKit.CONTENT_TYPE);

		this.editor.setFont(ScaleUtil.scaleFont(Config.currentFonts().editor.value()));
		this.editor.setCaretColor(syntaxColors.text.value());

		this.editor.setBackground(syntaxColors.editorBackground.value());
		// set unit increment to height of one line, the amount scrolled per
		// mouse wheel rotation is then controlled by OS settings
		this.editorScrollPane.getVerticalScrollBar().setUnitIncrement(this.editor.getFontMetrics(this.editor.getFont()).getHeight());

		this.decompilingLabel.setFont(ScaleUtil.getFont(this.decompilingLabel.getFont().getFontName(), Font.BOLD, 26));
		this.decompilingProgressBar.setIndeterminate(true);
		this.errorTextArea.setEditable(false);
		this.errorTextArea.setFont(ScaleUtil.getFont(Font.MONOSPACED, Font.PLAIN, 10));

		this.obfuscatedPainter = ThemeUtil.createObfuscatedPainter();
		this.proposedPainter = ThemeUtil.createProposedPainter();
		this.debugPainter = ThemeUtil.createDebugPainter();
		this.fallbackPainter = ThemeUtil.createFallbackPainter();
		this.deobfuscatedPainter = ThemeUtil.createDeobfuscatedPainter();

		this.retryButton.addActionListener(e -> this.redecompileClass());
	}

	protected abstract S createEditorScrollPane(JEditorPane editor);

	protected void installEditorRuler(int lineOffset) {
		final SyntaxPaneProperties.Colors syntaxColors = Config.getCurrentSyntaxPaneColors();

		final LineNumbersRuler ruler = LineNumbersRuler.install(new LineNumbersRuler(
				this.editor, syntaxColors.lineNumbersSelected.value(), lineOffset
		));
		ruler.setForeground(syntaxColors.lineNumbersForeground.value());
		ruler.setBackground(syntaxColors.lineNumbersBackground.value());
		ruler.setFont(this.editor.getFont());
	}

	/**
	 * @return a future whose completion indicates that this editor's class handle and source have been set
	 */
	public CompletableFuture<?> setClassHandle(ClassHandle handle) {
		return this.setClassHandle(handle, true, null);
	}

	protected CompletableFuture<?> setClassHandle(
			ClassHandle handle, boolean closeOldHandle,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		ClassEntry old = null;
		if (this.classHandler != null) {
			this.classHandler.removeListener();
			old = this.classHandler.getHandle().getRef();
			if (closeOldHandle) {
				this.classHandler.getHandle().close();
			}
		}

		return this.setClassHandleImpl(old, handle, snippetFactory);
	}

	protected CompletableFuture<?> setClassHandleImpl(
			ClassEntry old, ClassHandle handle,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		this.setDisplayMode(DisplayMode.IN_PROGRESS);
		this.setCursorReference(null);

		this.classHandler = ClassHandler.of(handle, new ClassHandleListener() {
			@Override
			public void onMappedSourceChanged(ClassHandle h, Result<DecompiledClassSource, ClassHandleError> res) {
				AbstractEditorPanel.this.handleDecompilerResult(res, snippetFactory);
			}

			@Override
			public void onInvalidate(ClassHandle h, InvalidationType t) {
				SwingUtilities.invokeLater(() -> {
					if (t == InvalidationType.FULL) {
						AbstractEditorPanel.this.setDisplayMode(DisplayMode.IN_PROGRESS);
					}
				});
			}
		});

		return handle.getSource()
			.thenApplyAsync(
					res -> this.handleDecompilerResult(res, snippetFactory),
					SwingUtilities::invokeLater
			)
			.thenAcceptAsync(CompletableFuture::join);
	}

	public void destroy() {
		this.classHandler.getHandle().close();
	}

	private void redecompileClass() {
		if (this.classHandler != null) {
			this.classHandler.getHandle().invalidate();
		}
	}

	private CompletableFuture<?> handleDecompilerResult(
			Result<DecompiledClassSource, ClassHandleError> res,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		return CompletableFuture.runAsync(
			() -> {
				if (res.isOk()) {
					this.setSource(res.unwrap(), snippetFactory);
				} else {
					this.displayError(res.unwrapErr());
				}

				this.nextReference = null;
			},
			SwingUtilities::invokeLater
		);
	}

	private void displayError(ClassHandleError t) {
		this.setDisplayMode(DisplayMode.ERRORED);
		String str = switch (t.type) {
			case DECOMPILE -> "editor.decompile_error";
			case REMAP -> "editor.remap_error";
		};
		this.errorLabel.setText(I18n.translate(str));
		this.errorTextArea.setText(t.getStackTrace());
		this.errorTextArea.setCaretPosition(0);
	}

	private void setDisplayMode(DisplayMode mode) {
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

				final JPanel editorPane = new JPanel() {
					@Override
					public boolean isOptimizedDrawingEnabled() {
						return false;
					}
				};
				editorPane.setLayout(new GridBagLayout());

				this.initEditorPane(editorPane);

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

	/**
	 * @see #consumeEditorMouseTarget(MouseTargetAction, Runnable)
	 */
	protected void consumeEditorMouseTarget(MouseTargetAction action) {
		this.consumeEditorMouseTarget(action, Runnables.doNothing());
	}

	/**
	 * If the mouse is currently over a {@link Token} in the {@link #editor} that resolves to an {@link Entry}, passes
	 * the token and entry to the passed {@code action},
	 * along with whether the entry is a resolved parent of the targeted entry.<br>
	 * Otherwise, calls the passed {@code onNoTarget}.
	 *
	 * @param action     the action to run when the mouse is over a token that resolves to an entry
	 * @param onNoTarget the action to run when the mouse is not over a token that resolves to an entry
	 */
	protected void consumeEditorMouseTarget(MouseTargetAction action, Runnable onNoTarget) {
		consumeMousePositionIn(
				this.editor,
				(absoluteMouse, relativeMouse) -> Optional.of(relativeMouse)
					.map(this.editor::viewToModel2D)
					.filter(textPos -> textPos >= 0)
					.map(this::getToken)
					.ifPresentOrElse(
						token -> Optional.of(token)
							.map(this::getReference)
							.ifPresentOrElse(
								reference -> {
									final Entry<?> resolved = this.resolveReference(reference);
									action.run(token, resolved, !resolved.equals(reference.entry));
								},
								onNoTarget
							),
						onNoTarget
					),
				ignored -> onNoTarget.run()
		);
	}

	protected void initEditorPane(JPanel editorPane) {
		editorPane.add(this.editorScrollPane, GridBagConstraintsBuilder.create()
				.pos(0, 0)
				.weight(1, 1)
				.fill(GridBagConstraints.BOTH)
				.build()
		);
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

	protected Entry<?> resolveReference(EntryReference<Entry<?>, Entry<?>> reference) {
		final Entry<?> navigationEntry;
		if (reference.context == null) {
			final EntryResolver resolver = this.controller.getProject().getRemapper().getObfResolver();
			navigationEntry = resolver.resolveFirstEntry(reference.entry, ResolutionStrategy.RESOLVE_ROOT);
		} else {
			navigationEntry = reference.entry;
		}

		return this.gui.getController().getProject().getRepresentative(navigationEntry);
	}

	protected void setCursorReference(EntryReference<Entry<?>, Entry<?>> ref) {
		this.cursorReference = ref;
	}

	public Token getToken(int pos) {
		if (this.source == null) {
			return null;
		}

		return this.source.getIndex().getReferenceToken(this.sourceBounds.offsetOf(pos));
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (this.source == null) {
			return null;
		}

		return this.source.getIndex().getReference(token);
	}

	protected void setSource(
			DecompiledClassSource source,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		this.setDisplayMode(DisplayMode.SUCCESS);
		if (source == null) {
			return;
		}

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

			final Snippet snippet = snippetFactory == null ? null : snippetFactory.apply(this.source);
			if (snippet == null) {
				this.editor.setText(this.source.toString());
				this.sourceBounds = new DefaultBounds();
			} else {
				final String sourceString = this.source.toString();

				final int end = Math.min(sourceString.length(), snippet.end);

				final Unindented unindented = Unindented.of(sourceString, snippet.start, end);

				this.sourceBounds = new TrimmedBounds(snippet.start, end, unindented.indentOffsets);
				this.editor.setText(unindented.snippet);

				newCaretPos = Utils.clamp((long) newCaretPos - this.sourceBounds.start(), 0, this.editor.getText().length());
			}

			this.setHighlightedTokens(source.getTokenStore(), source.getHighlightedTokens());
			this.editor.setCaretPosition(newCaretPos);

			for (final Consumer<DecompiledClassSource> listener : this.sourceSetListeners) {
				listener.accept(this.source);
			}

			this.setCursorReference(this.getReference(this.getToken(this.editor.getCaretPosition())));
		} finally {
			this.settingSource = false;
		}

		if (this.nextReference != null) {
			this.showReferenceImpl(this.nextReference);
			this.nextReference = null;
		}
	}

	protected void addSourceSetListener(Consumer<DecompiledClassSource> listener) {
		this.sourceSetListeners.add(listener);
	}

	protected void removeSourceSetListener(Consumer<DecompiledClassSource> listener) {
		this.sourceSetListeners.remove(listener);
	}

	protected boolean isBounded() {
		return this.sourceBounds instanceof TrimmedBounds;
	}

	public void setHighlightedTokens(TokenStore tokenStore, Map<TokenType, ? extends Collection<Token>> tokens) {
		// remove any old highlighters
		this.editor.getHighlighter().removeAllHighlights();

		for (TokenType type : tokens.keySet()) {
			BoxHighlightPainter typePainter = switch (type) {
				case OBFUSCATED -> this.obfuscatedPainter;
				case DEOBFUSCATED -> this.deobfuscatedPainter;
				case DEBUG -> this.debugPainter;
				case JAR_PROPOSED, DYNAMIC_PROPOSED -> this.proposedPainter;
			};

			for (Token token : tokens.get(type)) {
				BoxHighlightPainter tokenPainter = typePainter;
				EntryReference<Entry<?>, Entry<?>> reference = this.getReference(token);

				if (reference != null) {
					EditableType t = EditableType.fromEntry(reference.entry);
					boolean editable = t == null || this.gui.isEditable(t);
					boolean fallback = tokenStore.isFallback(token);
					tokenPainter = editable ? (fallback ? this.fallbackPainter : typePainter) : this.proposedPainter;
				}

				this.addHighlightedToken(token, tokenPainter);
			}
		}

		this.editor.validate();
		this.editor.repaint();
	}

	private void addHighlightedToken(Token token, HighlightPainter tokenPainter) {
		this.sourceBounds.offsetOf(token).ifPresent(offsetToken -> {
			try {
				this.editor.getHighlighter().addHighlight(offsetToken.start, offsetToken.end, tokenPainter);
			} catch (BadLocationException ex) {
				throw new IllegalArgumentException(ex);
			}
		});
	}

	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		return this.cursorReference;
	}

	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (this.mode == DisplayMode.SUCCESS) {
			this.showReferenceImpl(reference);
		} else if (this.mode != DisplayMode.ERRORED) {
			this.nextReference = reference;
		}
	}

	/**
	 * Navigates to the reference without modifying history. Assumes the class is loaded.
	 */
	private void showReferenceImpl(EntryReference<Entry<?>, Entry<?>> reference) {
		if (this.source == null || reference == null) {
			return;
		}

		final List<Token> tokens = this.getReferences(reference);

		if (tokens.isEmpty()) {
			Logger.debug("No tokens found for {} in {}", reference, this.classHandler.getHandle().getRef());
		} else {
			this.gui.showTokens(this, tokens);
		}
	}

	protected List<Token> getReferences(EntryReference<Entry<?>, Entry<?>> reference) {
		return Optional.of(this.controller.getTokensForReference(this.source, reference))
				.filter(directTokens -> !directTokens.isEmpty())
				.or(() -> {
					// record component getters often don't have a declaration token
					// try to get the field declaration instead
					return reference.entry instanceof MethodEntry method
							? getRecordIndexingService(this.gui)
									.map(service -> service.getComponentField(method))
									.map(field -> EntryReference.<Entry<?>, Entry<?>>declaration(field, field.getName()))
									.map(fieldReference -> this.controller.getTokensForReference(this.source, fieldReference))
							: Optional.empty();
				})
				.orElse(List.of());
	}

	/**
	 * Attempts navigating to and momentarily highlighting the passed {@code token}.
	 *
	 * @param token the token to navigate to, in {@linkplain #sourceBounds bounded} space
	 */
	public void navigateToToken(@Nullable Token token) {
		final Token boundedToken = this.navigateToTokenImpl(token);
		if (boundedToken == null) {
			return;
		}

		this.startHighlightingSelection(boundedToken);
	}

	private void startHighlightingSelection(Token token) {
		if (this.selectionHighlightHandler != null) {
			this.selectionHighlightHandler.finish();
		}

		final SelectionHighlightSection config = Config.editor().selectionHighlight;
		final int blinks = config.blinks.value();
		if (blinks > 0) {
			final EditorHighlightHandler handler =
					new EditorHighlightHandler(this, token, config.blinkDelay.value(), blinks);

			handler.start();

			this.selectionHighlightHandler = handler;
		}
	}

	/**
	 * @return a token equivalent to the passed {@code unBoundedToken} with its position shifted so it aligns with the
	 * bounded source if navigation was successful, or {@code null} otherwise
	 */
	@Nullable
	protected Token navigateToTokenImpl(@Nullable Token unBoundedToken) {
		if (unBoundedToken == null) {
			return null;
		}

		final Token boundedToken = this.sourceBounds.offsetOf(unBoundedToken).orElse(null);
		if (boundedToken == null) {
			// token out of bounds
			return null;
		}

		// set the caret position to the token
		this.editor.setCaretPosition(boundedToken.start);
		this.editor.grabFocus();

		try {
			// make sure the token is visible in the scroll window
			Rectangle2D start = this.editor.modelToView2D(boundedToken.start);
			Rectangle2D end = this.editor.modelToView2D(boundedToken.start);
			if (start == null || end == null) {
				return null;
			}

			Rectangle show = new Rectangle();
			Rectangle2D.union(start, end, show);
			show.grow((int) (start.getWidth() * 10), (int) (start.getHeight() * 6));
			SwingUtilities.invokeLater(() -> this.editor.scrollRectToVisible(show));
		} catch (BadLocationException ex) {
			if (!this.settingSource) {
				throw new RuntimeException(ex);
			} else {
				return null;
			}
		}

		return boundedToken;
	}

	protected Object addHighlight(Token token, HighlightPainter highlightPainter) {
		try {
			return AbstractEditorPanel.this.editor.getHighlighter()
					.addHighlight(token.start, token.end, highlightPainter);
		} catch (BadLocationException ex) {
			return null;
		}
	}

	protected SourceBounds getSourceBounds() {
		return this.sourceBounds;
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
		return this.classHandler == null ? null : this.classHandler.getHandle();
	}

	public String getSimpleClassName() {
		return this.getDeobfOrObfHandleRef().getSimpleName();
	}

	public String getFullClassName() {
		return this.getDeobfOrObfHandleRef().getFullName();
	}

	private ClassEntry getDeobfOrObfHandleRef() {
		final ClassEntry deobfRef = this.classHandler.handle.getDeobfRef();
		return deobfRef == null ? this.classHandler.handle.getRef() : deobfRef;
	}

	public record Snippet(int start, int end) {
		public Snippet {
			if (start < 0) {
				throw new IllegalArgumentException("start must not be negative!");
			}

			if (start > end) {
				throw new IllegalArgumentException("start must not be greater than end!");
			}
		}
	}

	private record LineOffset(int sourceStart, int sourceEnd, int offset) {
		boolean contains(int pos) {
			return this.sourceStart <= pos && pos < this.sourceEnd;
		}

		boolean contains(Token token) {
			return this.sourceStart <= token.start && token.end < this.sourceEnd;
		}
	}

	/**
	 * An unindented snippet of source code along with the data required
	 * to map tokens from their original position in the source code.
	 *
	 * @param snippet       the unindented code snippet
	 * @param indentOffsets the cumulative offset resulting from stripped indents for each line, if any
	 */
	private record Unindented(String snippet, ImmutableList<LineOffset> indentOffsets) {
		static Unindented ofNoIndent(String source, int start, int end) {
			return new Unindented(source.substring(start, end), ImmutableList.of());
		}

		/**
		 * Gets the unindented snippet of the passed {@code source} between the passed {@code start} and {@code end}.
		 *
		 * <p> The amount of indent is determined by looking for spaces/tabs <em>before</em> the passed {@code start}.
		 * If the first character before {@code start} is a tab, {@code source} is considered to be tab-indented,
		 * likewise for a space.
		 *
		 * <p> If any line is less indented than the first line, the whole snippet is considered to have <em>no</em>
		 * indent; that doesn't fit expected formatting.
		 */
		static Unindented of(String source, int start, int end) {
			if (start == 0) {
				return ofNoIndent(source, start, end);
			} else {
				final char indentChar = source.charAt(start - 1);
				if (indentChar == '\t' || indentChar == ' ') {
					final int firstLineIndent;
					{
						int currentIndent = 1;
						while (source.charAt(start - currentIndent - 1) == indentChar) {
							currentIndent++;
						}

						firstLineIndent = currentIndent;
					}

					final Matcher lineMatcher = LineIndexer.LINE_END.matcher(source);
					final var snippet = new StringBuilder();
					final ImmutableList.Builder<LineOffset> indents = ImmutableList.builder();

					int prevIndentEnd = start;
					int prevSourceLineStart = start;
					int indentOffset = 0;
					while (lineMatcher.find(prevIndentEnd)) {
						final int currentIndentEnd;
						if (lineMatcher.end() < end) {
							currentIndentEnd = lineMatcher.end() + firstLineIndent;
							for (int i = lineMatcher.end(); i < end && i < currentIndentEnd; i++) {
								if (source.charAt(i) != indentChar) {
									// if any line is indented less than the first, no indent
									return ofNoIndent(source, start, end);
								}
							}
						} else {
							snippet.append(source, prevIndentEnd, end);
							indents.add(new LineOffset(prevSourceLineStart, end, indentOffset));

							break;
						}

						snippet.append(source, prevIndentEnd, lineMatcher.end());
						indents.add(new LineOffset(prevSourceLineStart, lineMatcher.end(), indentOffset));

						prevIndentEnd = currentIndentEnd;
						prevSourceLineStart = currentIndentEnd - firstLineIndent;
						indentOffset += firstLineIndent;
					}

					return new Unindented(snippet.toString(), indents.build());
				} else {
					return ofNoIndent(source, start, end);
				}
			}
		}
	}

	protected sealed interface SourceBounds {
		int start();

		int end();

		default boolean contains(int pos) {
			return pos >= this.start() && pos <= this.end();
		}

		default boolean contains(Token token) {
			return this.start() <= token.start && token.end <= this.end();
		}

		int offsetOf(int unBoundedPos);

		Optional<Token> offsetOf(@Nullable Token boundedToken);
	}

	private record TrimmedBounds(int start, int end, ImmutableList<LineOffset> indentOffsets) implements SourceBounds {
		@Override
		public int offsetOf(int boundedPos) {
			final int searchStart = boundedPos + this.start;
			return this.indentOffsets().stream()
				.flatMap(indentOffset -> {
					final int potentialPos = searchStart + indentOffset.offset;
					return indentOffset.contains(potentialPos) ? Stream.of(potentialPos) : Stream.empty();
				})
				.findFirst()
				.orElse(searchStart);
		}

		@Override
		public Optional<Token> offsetOf(@Nullable Token unBoundedToken) {
			if (unBoundedToken == null || !this.contains(unBoundedToken)) {
				return Optional.empty();
			} else {
				final int offset = this.start() + this.indentOffsets().stream()
						.filter(lineOffset -> lineOffset.contains(unBoundedToken))
						.findFirst()
						.map(LineOffset::offset)
						.orElse(0);

				return Optional.of(unBoundedToken.move(-offset));
			}
		}
	}

	private final class DefaultBounds implements SourceBounds {
		@Override
		public int start() {
			return 0;
		}

		@Override
		public int end() {
			return AbstractEditorPanel.this.source.toString().length();
		}

		@Override
		public int offsetOf(int unBoundedPos) {
			return unBoundedPos;
		}

		@Override
		public Optional<Token> offsetOf(@Nullable Token boundedToken) {
			return boundedToken == null || this.end() < boundedToken.end ? Optional.empty() : Optional.of(boundedToken);
		}
	}

	private enum DisplayMode {
		INACTIVE,
		IN_PROGRESS,
		SUCCESS,
		ERRORED,
	}

	public static final class ClassHandler {
		public static ClassHandler of(ClassHandle handle, ClassHandleListener listener) {
			handle.addListener(listener);

			return new ClassHandler(handle, listener);
		}

		private final ClassHandle handle;
		private final ClassHandleListener listener;

		private ClassHandler(ClassHandle handle, ClassHandleListener listener) {
			this.handle = handle;
			this.listener = listener;
		}

		public ClassHandle getHandle() {
			return this.handle;
		}

		public void removeListener() {
			this.handle.removeListener(this.listener);
		}
	}

	@FunctionalInterface
	protected interface MouseTargetAction {
		void run(Token token, Entry<?> entry, boolean resolvedParent);
	}
}

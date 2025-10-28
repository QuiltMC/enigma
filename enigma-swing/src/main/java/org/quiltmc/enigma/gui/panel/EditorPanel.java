package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.event.ClassHandleListener;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindToolBar;
import org.quiltmc.enigma.gui.element.EditorPopupMenu;
import org.quiltmc.enigma.gui.element.NavigatorPanel;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.syntaxpain.DefaultSyntaxAction;
import org.quiltmc.syntaxpain.SyntaxDocument;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.KeyboardFocusManager;
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
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.text.JTextComponent;

import static org.quiltmc.enigma.gui.util.GuiUtil.consumeMousePositionIn;
import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;
import static javax.swing.SwingUtilities.isDescendingFrom;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public class EditorPanel extends BaseEditorPanel {
	private static final int MOUSE_STOPPED_MOVING_DELAY = 100;

	private final NavigatorPanel navigatorPanel;
	private final EnigmaQuickFindToolBar quickFindToolBar = new EnigmaQuickFindToolBar();
	private final EditorPopupMenu popupMenu;

	// DIY tooltip because JToolTip can't be moved or resized
	private final EntryTooltip entryTooltip = new EntryTooltip(this.gui);
	private final WindowAdapter guiLostFocusListener;

	@Nullable
	private Token lastMouseTargetToken;

	// avoid finding the mouse entry every mouse movement update
	private final Timer mouseStoppedMovingTimer = new Timer(MOUSE_STOPPED_MOVING_DELAY, e -> {
		if (Config.editor().entryTooltip.enable.value()) {
			this.consumeEditorMouseTarget(
					(token, entry, resolvedParent) -> {
						this.hideTooltipTimer.stop();
						if (this.entryTooltip.isVisible()) {
							this.showTooltipTimer.stop();

							if (!token.equals(this.lastMouseTargetToken)) {
								this.lastMouseTargetToken = token;
								this.openTooltip(entry, resolvedParent);
							}
						} else {
							this.lastMouseTargetToken = token;
							this.showTooltipTimer.start();
						}
					},
					() -> consumeMousePositionIn(
						this.entryTooltip.getContentPane(),
						(absolute, relative) -> this.hideTooltipTimer.stop(),
						absolute -> {
							this.lastMouseTargetToken = null;
							this.showTooltipTimer.stop();
							this.hideTooltipTimer.start();
						}
					)
			);
		}
	});

	private final Timer showTooltipTimer = new Timer(
			ToolTipManager.sharedInstance().getInitialDelay() - MOUSE_STOPPED_MOVING_DELAY, e -> {
				this.consumeEditorMouseTarget((token, entry, resolvedParent) -> {
					if (token.equals(this.lastMouseTargetToken)) {
						this.entryTooltip.setVisible(true);
						this.openTooltip(entry, resolvedParent);
					}
				});
			}
	);

	private final Timer hideTooltipTimer = new Timer(
			ToolTipManager.sharedInstance().getDismissDelay() - MOUSE_STOPPED_MOVING_DELAY,
			e -> this.entryTooltip.close()
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
		this.editor.setComponentPopupMenu(this.popupMenu.getUi());

		this.entryTooltip.addCloseListener(this::onTooltipClose);
		this.guiLostFocusListener = new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				if (e.getOppositeWindow() != EditorPanel.this.entryTooltip) {
					EditorPanel.this.entryTooltip.close();
				}
			}
		};
		this.gui.getFrame().addWindowFocusListener(this.guiLostFocusListener);

		this.editor.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e1) {
				if ((e1.getModifiersEx() & CTRL_DOWN_MASK) != 0 && e1.getButton() == MouseEvent.BUTTON1) {
					// ctrl + left click
					EditorPanel.this.navigateToCursorReference();
				}
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				EditorPanel.this.entryTooltip.close();
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

		this.editor.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!EditorPanel.this.entryTooltip.hasRepopulated()) {
					EditorPanel.this.mouseStoppedMovingTimer.restart();
				}
			}
		});

		this.editor.addCaretListener(event -> this.onCaretMove(event.getDot()));

		this.editorScrollPane.getViewport().addChangeListener(e -> this.entryTooltip.close());

		this.mouseStoppedMovingTimer.setRepeats(false);
		this.showTooltipTimer.setRepeats(false);
		this.hideTooltipTimer.setRepeats(false);

		this.entryTooltip.setVisible(false);

		this.entryTooltip.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (Config.editor().entryTooltip.interactable.value()) {
					EditorPanel.this.mouseStoppedMovingTimer.stop();
					EditorPanel.this.hideTooltipTimer.stop();
				}
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

	private void onTooltipClose() {
		this.lastMouseTargetToken = null;
		this.mouseStoppedMovingTimer.stop();
		this.showTooltipTimer.stop();
		this.hideTooltipTimer.stop();
	}

	private void openTooltip(Entry<?> target, boolean inherited) {
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		final Component eventReceiver = focusOwner != null && isDescendingFrom(focusOwner, this.gui.getFrame())
				? focusOwner : null;

		this.entryTooltip.open(target, inherited, eventReceiver);
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
		this.gui.getFrame().removeWindowFocusListener(this.guiLostFocusListener);
	}

	public NavigatorPanel getNavigatorPanel() {
		return this.navigatorPanel;
	}

	@Override
	protected void setClassHandleImpl(
			ClassEntry old, ClassHandle handle,
			@Nullable Function<DecompiledClassSource, Snippet> snippetFactory
	) {
		super.setClassHandleImpl(old, handle, snippetFactory);

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
		this.entryTooltip.setZoom(zoomAmount);
	}

	@Override
	public void resetEditorZoom() {
		super.resetEditorZoom();
		this.entryTooltip.resetZoom();
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

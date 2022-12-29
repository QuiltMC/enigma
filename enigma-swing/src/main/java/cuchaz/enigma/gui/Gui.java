/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.common.collect.Lists;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.*;
import cuchaz.enigma.gui.panels.*;
import cuchaz.enigma.gui.panels.right.CallsTree;
import cuchaz.enigma.gui.panels.right.ImplementationsTree;
import cuchaz.enigma.gui.panels.right.InheritanceTree;
import cuchaz.enigma.gui.panels.right.MessagesPanel;
import cuchaz.enigma.gui.panels.right.RightPanel;
import cuchaz.enigma.gui.panels.right.UsersPanel;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

public class Gui {

	private final MainWindow mainWindow;
	private final GuiController controller;

	private ConnectionState connectionState;
	private boolean isJarOpen;
	private final Set<EditableType> editableTypes;
	private boolean singleClassTree;

	private final MenuBar menuBar;
	private final ObfPanel obfPanel;
	private final DeobfPanel deobfPanel;
	private final IdentifierPanel infoPanel;

	private final EditorTabbedPane editorTabbedPane;

	private final JPanel classesPanel = new JPanel(new BorderLayout());
	private final JSplitPane splitClasses;
	private final JPanel centerPanel = new JPanel(new BorderLayout());
	private RightPanel rightPanel;
	private final JSplitPane splitRight;
	private final JSplitPane splitCenter;

	private final DefaultListModel<String> userModel = new DefaultListModel<>();
	private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
	private final JList<String> users = new JList<>(this.userModel);
	private final JList<Message> messages = new JList<>(this.messageModel);

	private final JLabel connectionStatusLabel = new JLabel();

	public final JFileChooser jarFileChooser = new JFileChooser();
	public final JFileChooser tinyMappingsFileChooser = new JFileChooser();
	public final JFileChooser enigmaMappingsFileChooser = new JFileChooser();
	public final JFileChooser exportSourceFileChooser = new JFileChooser();
	public final JFileChooser exportJarFileChooser = new JFileChooser();
	public SearchDialog searchDialog;

	public Gui(EnigmaProfile profile, Set<EditableType> editableTypes) {
		// right panels
		// top panels
		RightPanel.registerPanel(new CallsTree(this));
		RightPanel.registerPanel(new ImplementationsTree(this));
		RightPanel.registerPanel(new InheritanceTree(this));
		RightPanel.registerPanel(new StructurePanel(this));

		// bottom panels
		RightPanel.registerPanel(new MessagesPanel(this));
		RightPanel.registerPanel(new UsersPanel(this));

		// set default sizes for right panels
		for (RightPanel panel : RightPanel.panels.values()) {
			panel.getPanel().setPreferredSize(new Dimension(300, 100));
		}

		this.mainWindow = new MainWindow(this, Enigma.NAME);
		this.editableTypes = editableTypes;
		this.controller = new GuiController(this, profile);
		this.deobfPanel = new DeobfPanel(this);
		this.infoPanel = new IdentifierPanel(this);
		this.obfPanel = new ObfPanel(this);
		this.menuBar = new MenuBar(this);
		this.editorTabbedPane = new EditorTabbedPane(this);
		this.splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.obfPanel, this.deobfPanel);
		// todo hardcoded calls default
		this.rightPanel = RightPanel.getPanel("calls");
		this.rightPanel.getButton().setSelected(true);
		this.splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, rightPanel.getPanel());
		this.splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);

		this.setupUi();

		LanguageUtil.addListener(this::retranslateUi);
		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(this.getFrame()));

		this.mainWindow.setVisible(true);
	}

	private void setupUi() {
		this.jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.tinyMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.enigmaMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.enigmaMappingsFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.exportSourceFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportJarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.splitClasses.setResizeWeight(0.3);
		this.classesPanel.setPreferredSize(ScaleUtil.getDimension(250, 0));

		// layout controls
		Container workArea = this.mainWindow.workArea();
		workArea.setLayout(new BorderLayout());

		centerPanel.add(infoPanel.getUi(), BorderLayout.NORTH);
		centerPanel.add(this.editorTabbedPane.getUi(), BorderLayout.CENTER);

		messages.setCellRenderer(new MessageListCellRenderer());

		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		splitCenter.setResizeWeight(0); // let the right side take all the slack

		workArea.add(splitCenter, BorderLayout.CENTER);

		// restore state
		int[] layout = UiConfig.getLayout();
		if (layout.length >= 4) {
			this.splitClasses.setDividerLocation(layout[0]);
			this.splitCenter.setDividerLocation(layout[1]);
			this.splitRight.setDividerLocation(layout[2]);
		}

		this.mainWindow.statusBar().addPermanentComponent(this.connectionStatusLabel);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		JFrame frame = this.mainWindow.frame();
		frame.addWindowListener(GuiUtil.onWindowClose(e -> this.close()));

		frame.setSize(UiConfig.getWindowSize("Main Window", ScaleUtil.getDimension(1024, 576)));
		frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Point windowPos = UiConfig.getWindowPos("Main Window", null);
		if (windowPos != null) {
			frame.setLocation(windowPos);
		} else {
			frame.setLocationRelativeTo(null);
		}

		this.retranslateUi();
	}

	public RightPanel getRightPanel() {
		return this.rightPanel;
	}

	public void setRightPanel(String id) {
		this.rightPanel = RightPanel.getPanel(id);
		this.splitRight.setRightComponent(this.rightPanel.getPanel());
	}

	public MainWindow getMainWindow() {
		return this.mainWindow;
	}

	public JList<Message> getMessages() {
		return this.messages;
	}

	public JList<String> getUsers() {
		return this.users;
	}

	public JFrame getFrame() {
		return this.mainWindow.frame();
	}

	public GuiController getController() {
		return this.controller;
	}

	public void setSingleClassTree(boolean singleClassTree) {
		this.singleClassTree = singleClassTree;
		this.classesPanel.removeAll();
		this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		getController().refreshClasses();
		retranslateUi();
	}

	public boolean isSingleClassTree() {
		return singleClassTree;
	}

	public void onStartOpenJar() {
		this.classesPanel.removeAll();
		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		this.editorTabbedPane.closeAllEditorTabs();

		// update menu
		isJarOpen = true;

		updateUiState();
		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		this.editorTabbedPane.closeAllEditorTabs();
		this.classesPanel.removeAll();

		// update menu
		isJarOpen = false;
		setMappingsFile(null);

		updateUiState();
		redraw();
	}

	public EditorPanel openClass(ClassEntry entry) {
		return this.editorTabbedPane.openClass(entry);
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return this.editorTabbedPane.getActiveEditor();
	}

	public void closeEditor(EditorPanel editor) {
		this.editorTabbedPane.closeEditor(editor);
	}

	/**
	 * Navigates to the reference without modifying history. If the class is not currently loaded, it will be loaded.
	 *
	 * @param reference the reference
	 */
	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		this.editorTabbedPane.openClass(reference.getLocationClassEntry().getOutermostClass()).showReference(reference);
	}

	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		this.obfPanel.obfClasses.setClasses(obfClasses);
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.deobfPanel.deobfClasses.setClasses(deobfClasses);
	}

	public void setMappingsFile(Path path) {
		this.enigmaMappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		updateUiState();
	}

	public void showTokens(EditorPanel editor, List<Token> tokens) {
		this.setRightPanel("calls");

		if (tokens.size() > 1) {
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			((CallsTree) this.getRightPanel()).showTokens(tokens);
		} else {
			((CallsTree) this.getRightPanel()).clearTokens();
		}

		// show the first token
		editor.navigateToToken(tokens.get(0));
	}

	public void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		infoPanel.setReference(reference == null ? null : reference.entry);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		EditorPanel activeEditor = this.editorTabbedPane.getActiveEditor();
		return activeEditor == null ? null : activeEditor.getCursorReference();
	}

	public void startDocChange(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null || !this.isEditable(EditableType.JAVADOC)) return;
		JavadocDialog.show(mainWindow.frame(), getController(), cursorReference);
	}

	public void startRename(EditorPanel editor, String text) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		infoPanel.startRenaming(text);
	}

	public void startRename(EditorPanel editor) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		infoPanel.startRenaming();
	}

	public void showStructure(EditorPanel editor) {
		this.setRightPanel("structure");
		((StructurePanel) this.getRightPanel()).showStructure(editor);
	}

	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.setRightPanel("inheritance");
		((InheritanceTree) this.getRightPanel()).display(cursorReference.entry);
	}

	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.setRightPanel("implementations");
		((ImplementationsTree) this.getRightPanel()).display(cursorReference.entry);
	}

	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.setRightPanel("calls");
		((CallsTree) this.getRightPanel()).showCalls(cursorReference.entry, recurse);
	}

	public void toggleMapping(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		Entry<?> obfEntry = cursorReference.getNameableEntry();
		toggleMappingFromEntry(obfEntry);
	}

	public void toggleMappingFromEntry(Entry<?> obfEntry) {
		if (this.controller.project.getMapper().getDeobfMapping(obfEntry).targetName() != null) {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).clearDeobfName()));
		} else {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).withDefaultDeobfName(this.getController().project)));
		}
	}

	public void showDiscardDiag(IntFunction<Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.mainWindow.frame(), I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public CompletableFuture<Void> saveMapping() {
		if (this.enigmaMappingsFileChooser.getSelectedFile() != null || this.enigmaMappingsFileChooser.showSaveDialog(this.mainWindow.frame()) == JFileChooser.APPROVE_OPTION)
			return this.controller.saveMappings(this.enigmaMappingsFileChooser.getSelectedFile().toPath());
		return CompletableFuture.completedFuture(null);
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			exit();
		} else {
			// ask to save before closing
			showDiscardDiag(response -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping().thenRun(this::exit);
					// do not join, as join waits on swing to clear events
				} else if (response == JOptionPane.NO_OPTION) {
					exit();
				}

				return null;
			}, I18n.translate("prompt.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		UiConfig.setWindowPos("Main Window", this.mainWindow.frame().getLocationOnScreen());
		UiConfig.setWindowSize("Main Window", this.mainWindow.frame().getSize());
		UiConfig.setLayout(
				this.splitClasses.getDividerLocation(),
				this.splitCenter.getDividerLocation(),
				this.splitRight.getDividerLocation()
		);
		UiConfig.save();

		if (searchDialog != null) {
			searchDialog.dispose();
		}
		this.mainWindow.frame().dispose();
		System.exit(0);
	}

	public void redraw() {
		JFrame frame = this.mainWindow.frame();

		frame.validate();
		frame.repaint();
	}

	public void onRenameFromClassTree(ValidationContext vc, Object prevData, Object data, DefaultMutableTreeNode node) {
		if (data instanceof String) {
			// package rename
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
				ClassEntry prevDataChild = (ClassEntry) childNode.getUserObject();
				ClassEntry dataChild = new ClassEntry(data + "/" + prevDataChild.getSimpleName());

				onRenameFromClassTree(vc, prevDataChild, dataChild, node);
			}
			node.setUserObject(data);
			// Ob package will never be modified, just reload deob view
			this.deobfPanel.deobfClasses.reload();
		} else if (data instanceof ClassEntry entry) {
			// class rename

			// TODO optimize reverse class lookup, although it looks like it's
			//      fast enough for now
			EntryRemapper mapper = this.controller.project.getMapper();
			ClassEntry obf = mapper.getObfToDeobf().getAllEntries()
					.filter(ClassEntry.class::isInstance)
					.map(ClassEntry.class::cast)
					.filter(e -> mapper.deobfuscate(e).equals(entry))
					.findAny().orElse(entry);

			this.controller.applyChange(vc, EntryChange.modify(obf).withDeobfName(((ClassEntry) data).getFullName()));
		} else {
			throw new IllegalStateException(String.format("unhandled rename object data: '%s'", data));
		}
	}

	public void moveClassTree(Entry<?> obfEntry, String newName) {
		String oldEntry = obfEntry.getContainingClass().getPackageName();
		String newEntry = new ClassEntry(newName).getPackageName();
		moveClassTree(obfEntry, oldEntry == null, newEntry == null);
	}

	// TODO: getExpansionState will *not* actually update itself based on name changes!
	public void moveClassTree(Entry<?> obfEntry, boolean isOldOb, boolean isNewOb) {
		ClassEntry classEntry = obfEntry.getContainingClass();

		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState();
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState();

		// Ob -> deob
		if (!isNewOb) {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.removeEntry(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Deob -> ob
		else if (!isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.removeEntry(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Local move
		else if (isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.reload();
		} else {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.reload();
		}

		this.deobfPanel.deobfClasses.restoreExpansionState(stateDeobf);
		this.obfPanel.obfClasses.restoreExpansionState(stateObf);
	}

	public ObfPanel getObfPanel() {
		return obfPanel;
	}

	public DeobfPanel getDeobfPanel() {
		return deobfPanel;
	}

	public SearchDialog getSearchDialog() {
		if (searchDialog == null) {
			searchDialog = new SearchDialog(this);
		}
		return searchDialog;
	}

	public void addMessage(Message message) {
		JScrollBar verticalScrollBar = ((MessagesPanel) RightPanel.panels.get("messages")).getMessageScrollPane().getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		messageModel.addElement(message);

		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}

		this.mainWindow.statusBar().showMessage(message.translate(), 5000);
	}

	public void setUserList(List<String> users) {
		userModel.clear();
		users.forEach(userModel::addElement);
		connectionStatusLabel.setText(String.format(I18n.translate("status.connected_user_count"), users.size()));
	}

	public void sendMessage() {
		JTextField chatBox = ((MessagesPanel) RightPanel.panels.get("messages")).getChatBox();
		String text = chatBox.getText().trim();

		if (!text.isEmpty()) {
			getController().sendPacket(new MessageC2SPacket(text));
		}
		chatBox.setText("");
	}

	/**
	 * Updates the state of the UI elements (button text, enabled state, ...) to reflect the current program state.
	 * This is a central place to update the UI state to prevent multiple code paths from changing the same state,
	 * causing inconsistencies.
	 */
	public void updateUiState() {
		menuBar.updateUiState();
		this.connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));
	}

	public void retranslateUi() {
		this.jarFileChooser.setDialogTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setDialogTitle(I18n.translate("menu.file.export.jar"));
		// todo set titles here

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.obfPanel.retranslateUi();
		this.deobfPanel.retranslateUi();
		this.infoPanel.retranslateUi();
		this.editorTabbedPane.retranslateUi();
		// todo retranslate panels here
	}

	public void setConnectionState(ConnectionState state) {
		connectionState = state;
		updateUiState();
	}

	public boolean isJarOpen() {
		return isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	public boolean validateImmediateAction(Consumer<ValidationContext> op) {
		ValidationContext vc = new ValidationContext();
		op.accept(vc);
		if (!vc.canProceed()) {
			List<ParameterizedMessage> messages = vc.getMessages();
			String text = ValidatableUi.formatMessages(messages);
			JOptionPane.showMessageDialog(this.getFrame(), text, String.format("%d message(s)", messages.size()), JOptionPane.ERROR_MESSAGE);
		}
		return vc.canProceed();
	}

	public boolean isEditable(EditableType t) {
		return this.editableTypes.contains(t);
	}

	public void reloadKeyBinds() {
		this.menuBar.setKeyBinds();
		this.editorTabbedPane.reloadKeyBinds();
	}
}

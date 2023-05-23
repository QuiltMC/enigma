package cuchaz.enigma.gui;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.docker.NotificationsDocker;
import cuchaz.enigma.gui.elements.EditorTabbedPane;
import cuchaz.enigma.gui.elements.MainWindow;
import cuchaz.enigma.gui.elements.MenuBar;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.gui.panels.IdentifierPanel;
import cuchaz.enigma.gui.docker.ObfuscatedClassesDocker;
import cuchaz.enigma.gui.docker.CollabDocker;
import cuchaz.enigma.gui.docker.StructureDocker;
import cuchaz.enigma.gui.docker.CallsTreeDocker;
import cuchaz.enigma.gui.docker.ImplementationsTreeDocker;
import cuchaz.enigma.gui.docker.InheritanceTreeDocker;
import cuchaz.enigma.gui.docker.DeobfuscatedClassesDocker;
import cuchaz.enigma.gui.docker.AllClassesDocker;
import cuchaz.enigma.gui.docker.Dock;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.ServerMessage;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.annotation.Nullable;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

public class Gui {
	private final MainWindow mainWindow;
	private final GuiController controller;

	private ConnectionState connectionState;
	private boolean isJarOpen;
	private final Set<EditableType> editableTypes;

	private final MenuBar menuBar;
	private final IdentifierPanel infoPanel;

	private final EditorTabbedPane editorTabbedPane;

	private final JPanel centerPanel;
	private final Dock rightDock;
	private final Dock leftDock;
	private final JSplitPane splitRight;
	private final JSplitPane splitLeft;

	private final DefaultListModel<String> userModel;
	private final DefaultListModel<ServerMessage> messageModel;
	private final JList<String> users;
	private final JList<ServerMessage> messages;

	private final JLabel connectionStatusLabel;
	private final NotificationManager notificationManager;

	public final JFileChooser jarFileChooser;
	public final JFileChooser tinyMappingsFileChooser;
	public final JFileChooser enigmaMappingsFileChooser;
	public final JFileChooser exportSourceFileChooser;
	public final JFileChooser exportJarFileChooser;
	public final SearchDialog searchDialog;

	public Gui(EnigmaProfile profile, Set<EditableType> editableTypes, boolean visible) {
		this.mainWindow = new MainWindow(Enigma.NAME);
		this.centerPanel = new JPanel(new BorderLayout());
		this.editableTypes = editableTypes;
		this.controller = new GuiController(this, profile);
		this.infoPanel = new IdentifierPanel(this);
		this.menuBar = new MenuBar(this);
		this.userModel = new DefaultListModel<>();
		this.messageModel = new DefaultListModel<>();
		this.users = new JList<>(this.userModel);
		this.messages = new JList<>(this.messageModel);
		this.editorTabbedPane = new EditorTabbedPane(this);
		this.rightDock = new Dock(this, Docker.Side.RIGHT);
		this.leftDock = new Dock(this, Docker.Side.LEFT);
		this.splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.centerPanel, this.rightDock);
		this.splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.leftDock, this.splitRight);
		this.jarFileChooser = new JFileChooser();
		this.tinyMappingsFileChooser = new JFileChooser();
		this.enigmaMappingsFileChooser = new JFileChooser();
		this.exportSourceFileChooser = new JFileChooser();
		this.exportJarFileChooser = new JFileChooser();
		this.connectionStatusLabel = new JLabel();
		this.notificationManager = new NotificationManager(this);
		this.searchDialog = new SearchDialog(this);

		this.setupUi();

		LanguageUtil.addListener(this::retranslateUi);
		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(this.getFrame()));

		this.mainWindow.setVisible(visible);
	}

	private void setupDockers() {
		// right dockers
		// top
		Docker.addDocker(new StructureDocker(this));
		Docker.addDocker(new InheritanceTreeDocker(this));
		Docker.addDocker(new ImplementationsTreeDocker(this));
		Docker.addDocker(new CallsTreeDocker(this));

		// bottom
		Docker.addDocker(new CollabDocker(this));
		Docker.addDocker(new NotificationsDocker(this));

		// left dockers
		// top
		Docker.addDocker(new ObfuscatedClassesDocker(this));
		Docker.addDocker(new AllClassesDocker(this));

		// bottom
		Docker.addDocker(new DeobfuscatedClassesDocker(this));

		// set default docker sizes
		for (Docker panel : Docker.getDockers().values()) {
			panel.setPreferredSize(new Dimension(300, 100));
		}

		// set up selectors
		for (Docker.Side side : Docker.Side.values()) {
			this.mainWindow.getDockerSelector(side).configure();
		}
	}

	private void setupUi() {
		this.setupDockers();

		this.jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.tinyMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.enigmaMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.enigmaMappingsFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.exportSourceFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportJarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		// layout controls
		Container workArea = this.mainWindow.getWorkArea();
		workArea.setLayout(new BorderLayout());

		this.centerPanel.add(this.infoPanel.getUi(), BorderLayout.NORTH);
		this.centerPanel.add(this.editorTabbedPane.getUi(), BorderLayout.CENTER);

		this.messages.setCellRenderer(new MessageListCellRenderer());

		workArea.add(this.splitLeft, BorderLayout.CENTER);

		this.mainWindow.getStatusBar().addPermanentComponent(this.connectionStatusLabel);

		// ensure that the center panel gets all extra resize space
		// this prevents the right and left panels from getting far too big occasionally
		this.splitRight.setResizeWeight(1);
		this.splitLeft.setResizeWeight(0);

		// apply docker config
		if (UiConfig.getHostedDockers(Docker.Side.LEFT).isPresent() || UiConfig.getHostedDockers(Docker.Side.RIGHT).isPresent()) {
			// restore
			this.rightDock.restoreState();
			this.leftDock.restoreState();
		} else {
			// use default config
			this.leftDock.host(Docker.getDocker(ObfuscatedClassesDocker.class), Docker.VerticalLocation.TOP);
			this.leftDock.host(Docker.getDocker(DeobfuscatedClassesDocker.class), Docker.VerticalLocation.BOTTOM);

			this.rightDock.host(Docker.getDocker(StructureDocker.class), Docker.VerticalLocation.FULL);
		}

		// init state
		this.setConnectionState(ConnectionState.NOT_CONNECTED);
		this.onCloseJar();

		JFrame frame = this.mainWindow.getFrame();
		frame.addWindowListener(GuiUtil.onWindowClose(e -> this.close()));

		frame.setSize(UiConfig.getWindowSize(UiConfig.MAIN_WINDOW, ScaleUtil.getDimension(1024, 576)));
		frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Point windowPos = UiConfig.getWindowPos(UiConfig.MAIN_WINDOW, null);
		if (windowPos != null) {
			frame.setLocation(windowPos);
		} else {
			frame.setLocationRelativeTo(null);
		}

		this.retranslateUi();
	}

	/**
	 * Opens the given docker in its preferred location.
	 * @param clazz the new panel's class
	 */
	public void openDocker(Class<? extends Docker> clazz) {
		Docker newDocker = Docker.getDocker(clazz);

		Dock dock = (newDocker.getButtonLocation().side() == Docker.Side.LEFT ? this.leftDock : this.rightDock);
		dock.host(newDocker, newDocker.getButtonLocation().verticalLocation());
	}

	public NotificationManager getNotificationManager() {
		return this.notificationManager;
	}

	public JSplitPane getSplitLeft() {
		return this.splitLeft;
	}

	public JSplitPane getSplitRight() {
		return this.splitRight;
	}

	public MenuBar getMenuBar() {
		return this.menuBar;
	}

	public MainWindow getMainWindow() {
		return this.mainWindow;
	}

	public JList<ServerMessage> getMessages() {
		return this.messages;
	}

	public JList<String> getUsers() {
		return this.users;
	}

	public JFrame getFrame() {
		return this.mainWindow.getFrame();
	}

	public GuiController getController() {
		return this.controller;
	}

	public void onStartOpenJar() {
		this.redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME + " - " + jarName);
		this.editorTabbedPane.closeAllEditorTabs();

		// update menu
		this.isJarOpen = true;

		// update classes in dockers
		this.controller.refreshClasses();

		this.updateUiState();
		this.redraw();
	}

	public void onCloseJar() {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME);
		this.setObfClasses(null);
		this.setDeobfClasses(null);
		this.editorTabbedPane.closeAllEditorTabs();

		// update menu
		this.isJarOpen = false;
		this.setMappingsFile(null);

		this.updateUiState();
		this.redraw();
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
		Docker.getDocker(ObfuscatedClassesDocker.class).getClassSelector().setClasses(obfClasses);
		this.updateAllClasses();
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		Docker.getDocker(DeobfuscatedClassesDocker.class).getClassSelector().setClasses(deobfClasses);
		this.updateAllClasses();
	}

	public void updateAllClasses() {
		ClassSelector allClasses = Docker.getDocker(AllClassesDocker.class).getClassSelector();

		List<ClassEntry> entries = new ArrayList<>();
		NestedPackages obfuscatedPackages = Docker.getDocker(DeobfuscatedClassesDocker.class).getClassSelector().getPackageManager();
		NestedPackages deobfuscatedPackages = Docker.getDocker(ObfuscatedClassesDocker.class).getClassSelector().getPackageManager();

		if (obfuscatedPackages != null) {
			entries.addAll(obfuscatedPackages.getClassEntries());
		}

		if (deobfuscatedPackages != null) {
			entries.addAll(deobfuscatedPackages.getClassEntries());
		}

		allClasses.setClasses(entries.isEmpty() || this.getController().getProject() == null ? null : entries);
	}

	public void setMappingsFile(Path path) {
		this.enigmaMappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		this.updateUiState();
	}

	public void showTokens(EditorPanel editor, List<Token> tokens) {
		if (tokens.size() > 1) {
			this.openDocker(CallsTreeDocker.class);
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			Docker.getDocker(CallsTreeDocker.class).showTokens(tokens);
		} else {
			Docker.getDocker(CallsTreeDocker.class).clearTokens();
		}

		// show the first token
		editor.navigateToToken(tokens.get(0));
	}

	public void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		this.infoPanel.setReference(reference == null ? null : reference.entry);
	}

	public void startDocChange(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null || !this.isEditable(EditableType.JAVADOC)) return;
		JavadocDialog.show(this.mainWindow.getFrame(), this.getController(), cursorReference);
	}

	public void startRename(EditorPanel editor, String text) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		this.infoPanel.startRenaming(text);
	}

	public void startRename(EditorPanel editor) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		this.infoPanel.startRenaming();
	}

	/**
	 * Updates the Structure docker without opening it.
	 * @param editor the editor to extract the new structure from
	 */
	public void updateStructure(EditorPanel editor) {
		Docker.getDocker(StructureDocker.class).updateStructure(editor);
	}

	/**
	 * Opens the Inheritance docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(InheritanceTreeDocker.class);
		Docker.getDocker(InheritanceTreeDocker.class).display(cursorReference.entry);
	}

	/**
	 * Opens the Implementations docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(ImplementationsTreeDocker.class);
		Docker.getDocker(ImplementationsTreeDocker.class).display(cursorReference.entry);
	}

	/**
	 * Opens the Calls docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(CallsTreeDocker.class);
		Docker.getDocker(CallsTreeDocker.class).showCalls(cursorReference.entry, recurse);
	}

	public void toggleMapping(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) {
			return;
		}

		Entry<?> obfEntry = cursorReference.getNameableEntry();
		this.toggleMappingFromEntry(obfEntry);
	}

	public void toggleMappingFromEntry(Entry<?> obfEntry) {
		if (this.controller.getProject().getMapper().getDeobfMapping(obfEntry).targetName() != null) {
			this.controller.applyChange(new ValidationContext(this.getNotificationManager()), EntryChange.modify(obfEntry).clearDeobfName());
		} else {
			this.controller.applyChange(new ValidationContext(this.getNotificationManager()), EntryChange.modify(obfEntry).withDefaultDeobfName(this.getController().getProject()));
		}
	}

	public void showDiscardDiag(IntFunction<Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.mainWindow.getFrame(), I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public CompletableFuture<Void> saveMapping() {
		if (this.enigmaMappingsFileChooser.getSelectedFile() != null || this.enigmaMappingsFileChooser.showSaveDialog(this.mainWindow.getFrame()) == JFileChooser.APPROVE_OPTION) {
			return this.controller.saveMappings(this.enigmaMappingsFileChooser.getSelectedFile().toPath());
		}

		return CompletableFuture.completedFuture(null);
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			this.exit();
		} else {
			// ask to save before closing
			this.showDiscardDiag(response -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping().thenRun(this::exit);
					// do not join, as join waits on swing to clear events
				} else if (response == JOptionPane.NO_OPTION) {
					this.exit();
				}

				return null;
			}, I18n.translate("prompt.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		UiConfig.setWindowPos(UiConfig.MAIN_WINDOW, this.mainWindow.getFrame().getLocationOnScreen());
		UiConfig.setWindowSize(UiConfig.MAIN_WINDOW, this.mainWindow.getFrame().getSize());

		// save state for docker panels
		this.rightDock.saveState();
		this.leftDock.saveState();

		UiConfig.save();

		if (this.searchDialog != null) {
			this.searchDialog.dispose();
		}

		this.mainWindow.getFrame().dispose();
		System.exit(0);
	}

	public void redraw() {
		JFrame frame = this.mainWindow.getFrame();

		frame.validate();
		frame.repaint();
	}

	public void moveClassTree(Entry<?> obfEntry, boolean updateSwingState, boolean isOldOb, boolean isNewOb) {
		ClassEntry classEntry = obfEntry.getContainingClass();

		ClassSelector deobfuscatedClassSelector = Docker.getDocker(DeobfuscatedClassesDocker.class).getClassSelector();
		ClassSelector obfuscatedClassSelector = Docker.getDocker(ObfuscatedClassesDocker.class).getClassSelector();

		List<ClassSelector.StateEntry> deobfuscatedPanelExpansionState = deobfuscatedClassSelector.getExpansionState();
		List<ClassSelector.StateEntry> obfuscatedPanelExpansionState = obfuscatedClassSelector.getExpansionState();

		if (!isNewOb && isOldOb) {
			// obfuscated -> deobfuscated
			obfuscatedClassSelector.removeEntry(classEntry);
			deobfuscatedClassSelector.moveClassIn(classEntry);
			if (updateSwingState) {
				obfuscatedClassSelector.reload();
				deobfuscatedClassSelector.reload();
			}
		} else if (!isOldOb && isNewOb) {
			// deobfuscated -> obfuscated
			deobfuscatedClassSelector.removeEntry(classEntry);
			obfuscatedClassSelector.moveClassIn(classEntry);
			if (updateSwingState) {
				obfuscatedClassSelector.reload();
				deobfuscatedClassSelector.reload();
			}
		}

		this.reloadClassEntry(classEntry, updateSwingState);

		if (updateSwingState) {
			deobfuscatedClassSelector.restoreExpansionState(deobfuscatedPanelExpansionState);
			obfuscatedClassSelector.restoreExpansionState(obfuscatedPanelExpansionState);
		}
	}

	public void reloadClassEntry(ClassEntry classEntry, boolean updateSwingState) {
		ClassSelector allClassesSelector = Docker.getDocker(AllClassesDocker.class).getClassSelector();

		if (updateSwingState) {
			Docker.getDocker(DeobfuscatedClassesDocker.class).getClassSelector().updateIfPresent(classEntry);
			Docker.getDocker(ObfuscatedClassesDocker.class).getClassSelector().updateIfPresent(classEntry);
			allClassesSelector.updateIfPresent(classEntry);
		}

		List<ClassSelector.StateEntry> expansionState = allClassesSelector.getExpansionState();
		if (updateSwingState) {
			allClassesSelector.reload();
			allClassesSelector.restoreExpansionState(expansionState);
		} else {
			allClassesSelector.moveClassIn(classEntry);
		}
	}

	public SearchDialog getSearchDialog() {
		return this.searchDialog;
	}

	public void addMessage(ServerMessage message) {
		JScrollBar verticalScrollBar = Docker.getDocker(CollabDocker.class).getMessageScrollPane().getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		this.messageModel.addElement(message);

		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}

		// popup notifications
		switch (message.getType()) {
			case CHAT -> {
				if (UiConfig.getServerNotificationLevel().equals(NotificationManager.ServerNotificationLevel.FULL) && !message.user.equals(NetConfig.getUsername())) {
					this.notificationManager.notify(new ParameterizedMessage(Message.MULTIPLAYER_CHAT, message.translate()));
				}
			}
			case CONNECT -> {
				if (UiConfig.getServerNotificationLevel() != NotificationManager.ServerNotificationLevel.NONE) {
					this.notificationManager.notify(new ParameterizedMessage(Message.MULTIPLAYER_USER_CONNECTED, message.translate()));
				}
			}
			case DISCONNECT -> {
				if (UiConfig.getServerNotificationLevel() != NotificationManager.ServerNotificationLevel.NONE) {
					this.notificationManager.notify(new ParameterizedMessage(Message.MULTIPLAYER_USER_LEFT, message.translate()));
				}
			}
		}

		this.mainWindow.getStatusBar().showMessage(message.translate(), NotificationManager.TIMEOUT_MILLISECONDS);
	}

	public void setUserList(List<String> users) {
		boolean wasOffline = this.isOffline();

		this.userModel.clear();
		users.forEach(this.userModel::addElement);
		this.connectionStatusLabel.setText(String.format(I18n.translate("status.connected_user_count"), users.size()));

		// if we were previously offline, we need to reload multiplayer-restricted dockers (only collab for now) so they can be used
		CollabDocker collabDocker = Docker.getDocker(CollabDocker.class);
		if (wasOffline && Dock.Util.isDocked(collabDocker)) {
			collabDocker.setUp();
		}
	}

	public boolean isOffline() {
		return this.getUsers().getModel().getSize() <= 0;
	}

	/**
	 * Updates the state of the UI elements (button text, enabled state, ...) to reflect the current program state.
	 * This is a central place to update the UI state to prevent multiple code paths from changing the same state,
	 * causing inconsistencies.
	 */
	public void updateUiState() {
		this.menuBar.updateUiState();
		this.connectionStatusLabel.setText(I18n.translate(this.connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));
	}

	public void retranslateUi() {
		this.jarFileChooser.setDialogTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setDialogTitle(I18n.translate("menu.file.export.jar"));

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.infoPanel.retranslateUi();
		this.editorTabbedPane.retranslateUi();
		for (Docker panel : Docker.getDockers().values()) {
			panel.retranslateUi();
		}
	}

	public void setConnectionState(ConnectionState state) {
		this.connectionState = state;
		this.updateUiState();
	}

	public boolean isJarOpen() {
		return this.isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	public boolean isEditable(EditableType t) {
		return this.editableTypes.contains(t);
	}

	public void reloadKeyBinds() {
		this.menuBar.setKeyBinds();
		this.editorTabbedPane.reloadKeyBinds();
	}

	public void openMostRecentFiles() {
		var pair = UiConfig.getMostRecentFilePair();

		if (pair.isPresent()) {
			this.getNotificationManager().notify(ParameterizedMessage.openedProject(pair.get().a().toString(), pair.get().b().toString()));
			this.controller.openJar(pair.get().a()).whenComplete((v, t) -> this.controller.openMappings(pair.get().b()));
		}
	}
}

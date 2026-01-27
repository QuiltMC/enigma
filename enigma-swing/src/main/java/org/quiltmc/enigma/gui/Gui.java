package org.quiltmc.enigma.gui;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.JavadocDialog;
import org.quiltmc.enigma.gui.dialog.SearchDialog;
import org.quiltmc.enigma.gui.docker.AllClassesDocker;
import org.quiltmc.enigma.gui.docker.CallsTreeDocker;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.docker.CollabDocker;
import org.quiltmc.enigma.gui.docker.DeobfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.Dock;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.quiltmc.enigma.gui.docker.ImplementationsTreeDocker;
import org.quiltmc.enigma.gui.docker.InheritanceTreeDocker;
import org.quiltmc.enigma.gui.docker.NotificationsDocker;
import org.quiltmc.enigma.gui.docker.ObfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.StructureDocker;
import org.quiltmc.enigma.gui.element.EditorTabbedPane;
import org.quiltmc.enigma.gui.element.MainWindow;
import org.quiltmc.enigma.gui.element.menu_bar.MenuBar;
import org.quiltmc.enigma.gui.panel.AbstractEditorPanel;
import org.quiltmc.enigma.gui.panel.EditorPanel;
import org.quiltmc.enigma.gui.panel.IdentifierPanel;
import org.quiltmc.enigma.gui.renderer.MessageListCellRenderer;
import org.quiltmc.enigma.gui.util.ExtensionFileFilter;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.LanguageUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.network.ServerMessage;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;
import org.quiltmc.enigma.util.validation.ValidationContext;

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
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public class Gui {
	private static final int DEFAULT_MIN_TOP_ON_SCREEN = 200;

	private final MainWindow mainWindow;
	private final GuiController controller;

	private ConnectionState connectionState;
	private boolean isJarOpen;
	private boolean showsProgressBars;
	private final Set<EditableType> editableTypes;
	private final List<Throwable> crashHistory;

	private final MenuBar menuBar;
	private final IdentifierPanel infoPanel;

	private final EditorTabbedPane editorTabbedPane;

	private final JPanel centerPanel;
	private final JSplitPane splitRight;
	private final JSplitPane splitLeft;
	private final DockerManager dockerManager;

	private final DefaultListModel<String> userModel;
	private final DefaultListModel<ServerMessage> messageModel;
	private final JList<String> users;
	private final JList<ServerMessage> messages;

	private final JLabel connectionStatusLabel;
	private final NotificationManager notificationManager;

	public final JFileChooser jarFileChooser;
	public final JFileChooser mappingsFileChooser;
	public final JFileChooser exportSourceFileChooser;
	public final JFileChooser exportJarFileChooser;
	public final SearchDialog searchDialog;

	private final boolean testEnvironment;

	/**
	 * Executor for {@link #reloadStats(ClassEntry, boolean) reloadStats} work.
	 *
	 * <p> Executes all work from one call to {@link #reloadStats(ClassEntry, boolean) reloadStats}
	 * before starting work for the next call.
	 * Fixes <a href="https://github.com/QuiltMC/enigma/issues/271">#271</a>.
	 */
	private final Executor reloadStatsExecutor = Executors.newSingleThreadExecutor();
	/**
	 * Setting this to true cancels unstarted work from the last call to
	 * {@link #reloadStats(ClassEntry, boolean) reloadStats}.
	 */
	private AtomicBoolean priorReloadStatsCanceler = new AtomicBoolean(false);

	public Gui(EnigmaProfile profile, Set<EditableType> editableTypes, boolean testEnvironment) {
		this.dockerManager = new DockerManager(this);
		this.mainWindow = new MainWindow(this, Enigma.NAME);
		this.centerPanel = new JPanel(new BorderLayout());
		this.editableTypes = editableTypes;
		this.controller = new GuiController(this, profile);
		this.infoPanel = new IdentifierPanel(this);
		this.crashHistory = new ArrayList<>();
		this.menuBar = new MenuBar(this);
		this.userModel = new DefaultListModel<>();
		this.messageModel = new DefaultListModel<>();
		this.users = new JList<>(this.userModel);
		this.messages = new JList<>(this.messageModel);
		this.editorTabbedPane = new EditorTabbedPane(this);
		this.splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.centerPanel, this.dockerManager.getRightDock());
		this.splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.dockerManager.getLeftDock(), this.splitRight);
		this.jarFileChooser = new JFileChooser();
		this.mappingsFileChooser = new JFileChooser();
		this.exportSourceFileChooser = new JFileChooser();
		this.exportJarFileChooser = new JFileChooser();
		this.connectionStatusLabel = new JLabel();
		this.notificationManager = new NotificationManager(this);
		this.searchDialog = new SearchDialog(this);
		this.testEnvironment = testEnvironment;

		this.showsProgressBars = true;

		this.setupUi();

		LanguageUtil.addListener(this::retranslateUi);

		this.mainWindow.setVisible(!testEnvironment);
	}

	private void setupDockers() {
		this.dockerManager.registerDocker(new StructureDocker(this));
		this.dockerManager.registerDocker(new InheritanceTreeDocker(this));
		this.dockerManager.registerDocker(new ImplementationsTreeDocker(this));
		this.dockerManager.registerDocker(new CallsTreeDocker(this));
		this.dockerManager.registerDocker(new CollabDocker(this));
		this.dockerManager.registerDocker(new NotificationsDocker(this));
		this.dockerManager.registerDocker(new ObfuscatedClassesDocker(this));
		this.dockerManager.registerDocker(new AllClassesDocker(this));
		this.dockerManager.registerDocker(new DeobfuscatedClassesDocker(this));

		if (Config.docker().buttonLocations.value().isEmpty()) {
			Config.docker().updateButtonLocations(this.dockerManager);
		}

		// set default docker sizes
		for (Docker docker : this.dockerManager.getDockers()) {
			docker.setPreferredSize(new Dimension(300, 100));
		}

		// set up selectors
		for (Docker.Side side : Docker.Side.values()) {
			this.mainWindow.getDockerSelector(side).configure();
		}
	}

	private void setupUi() {
		// fix folder icons being automatically hidden: https://github.com/JFormDesigner/FlatLaf/pull/609
		UIManager.put("Tree.showDefaultIcons", true);

		this.setupDockers();

		this.jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

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

		this.dockerManager.restoreStateFromConfig();

		// init state
		this.setConnectionState(ConnectionState.NOT_CONNECTED);
		this.onCloseJar();

		JFrame frame = this.mainWindow.getFrame();
		frame.addWindowListener(GuiUtil.onWindowClose(e -> this.close()));

		frame.setSize(Config.main().windowSize.value().toDimension());
		frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		final Point windowPos = Config.main().windowPos.value().toPoint();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		// keep the whole width (or as much as will fit) on-screen so window controls in the title bar are accessible
		final int clampedX = Utils.clamp(windowPos.x, 0, Math.max(0, screenSize.width - frame.getWidth()));
		// allow some of the bottom to be off-screen, but not the top, because of the title bar
		final int clampedY = Utils.clamp(windowPos.y, 0, screenSize.height - ScaleUtil.scale(DEFAULT_MIN_TOP_ON_SCREEN));

		if (windowPos.x != clampedX || windowPos.y != clampedY) {
			windowPos.x = clampedX;
			windowPos.y = clampedY;

			Config.main().windowPos.setValue(Config.Vec2i.fromPoint(windowPos));
		}

		frame.setLocation(windowPos);

		this.retranslateUi();
	}

	/**
	 * Opens the given docker in its preferred location.
	 * @param clazz the new panel's class
	 */
	public void openDocker(Class<? extends Docker> clazz) {
		Docker docker = this.dockerManager.getDocker(clazz);
		this.dockerManager.host(docker, docker.getButtonLocation());
	}

	public void setShowsProgressBars(boolean show) {
		this.showsProgressBars = show;
	}

	public boolean showsProgressBars() {
		return this.showsProgressBars;
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

	public List<Throwable> getCrashHistory() {
		return this.crashHistory;
	}

	public Set<EditableType> getEditableTypes() {
		return this.editableTypes;
	}

	public Set<StatType> getEditableStatTypes() {
		return EditableType.toStatTypes(this.getEditableTypes());
	}

	public boolean isTestEnvironment() {
		return this.testEnvironment;
	}

	public void addCrash(Throwable t) {
		this.crashHistory.add(t);
		this.menuBar.getFileMenu().updateState();
	}

	public DockerManager getDockerManager() {
		return this.dockerManager;
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
		this.dockerManager.getDocker(ObfuscatedClassesDocker.class).getClassSelector().setClasses(obfClasses);
		this.updateAllClasses();
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.dockerManager.getDocker(DeobfuscatedClassesDocker.class).getClassSelector().setClasses(deobfClasses);
		this.updateAllClasses();
	}

	public void updateAllClasses() {
		ClassSelector allClasses = this.dockerManager.getDocker(AllClassesDocker.class).getClassSelector();

		List<ClassEntry> entries = new ArrayList<>();
		NestedPackages obfuscatedPackages = this.dockerManager.getDocker(DeobfuscatedClassesDocker.class).getClassSelector().getPackageManager();
		NestedPackages deobfuscatedPackages = this.dockerManager.getDocker(ObfuscatedClassesDocker.class).getClassSelector().getPackageManager();

		if (obfuscatedPackages != null) {
			entries.addAll(obfuscatedPackages.getClassEntries());
		}

		if (deobfuscatedPackages != null) {
			entries.addAll(deobfuscatedPackages.getClassEntries());
		}

		allClasses.setClasses(entries.isEmpty() || this.getController().getProject() == null ? null : entries);
	}

	public void setMappingsFile(Path path) {
		this.mappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		this.updateUiState();
	}

	public void showTokens(AbstractEditorPanel<?> editor, List<Token> tokens) {
		if (tokens.size() > 1) {
			this.openDocker(CallsTreeDocker.class);
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			this.dockerManager.getDocker(CallsTreeDocker.class).showTokens(tokens);
		} else {
			this.dockerManager.getDocker(CallsTreeDocker.class).clearTokens();
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
		this.dockerManager.getDocker(StructureDocker.class).updateStructure(editor);
	}

	/**
	 * Opens the Inheritance docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(InheritanceTreeDocker.class);
		this.dockerManager.getDocker(InheritanceTreeDocker.class).display(cursorReference.entry);
	}

	/**
	 * Opens the Implementations docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(ImplementationsTreeDocker.class);
		this.dockerManager.getDocker(ImplementationsTreeDocker.class).display(cursorReference.entry);
	}

	/**
	 * Opens the Calls docker and displays information for the provided editor's cursor reference.
	 * @param editor the editor to extract the reference from
	 */
	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.openDocker(CallsTreeDocker.class);
		this.dockerManager.getDocker(CallsTreeDocker.class).showCalls(cursorReference.entry, recurse);
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
		EntryRemapper remapper = this.controller.getProject().getRemapper();
		Entry<?> resolvedEntry = remapper.getObfResolver().resolveFirstEntry(obfEntry, ResolutionStrategy.RESOLVE_ROOT);
		EntryMapping mapping = remapper.getMapping(resolvedEntry);

		EntryChange<?> change = EntryChange.modify(resolvedEntry);
		if (mapping.targetName() != null) {
			if (mapping.tokenType().isProposed()) {
				change = change.withTokenType(TokenType.DEOBFUSCATED).clearSourcePluginId();
			} else {
				EntryMapping proposedMapping = remapper.getProposedMappings().get(resolvedEntry);
				if (proposedMapping != null) {
					change = change.withDeobfName(proposedMapping.targetName()).withTokenType(proposedMapping.tokenType()).withSourcePluginId(proposedMapping.sourcePluginId());
				} else {
					change = change.clearDeobfName();
				}
			}
		} else {
			change = change.withDeobfName(obfEntry.getName());
		}

		this.controller.applyChange(new ValidationContext(this.getNotificationManager()), change);
	}

	public void showDiscardDiag(IntFunction<Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.mainWindow.getFrame(), I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public CompletableFuture<Void> saveMapping() {
		ExtensionFileFilter.setupFileChooser(this.getController().getGui(), this.mappingsFileChooser, this.controller.getReadWriteService());

		if (this.mappingsFileChooser.getSelectedFile() != null) {
			return this.controller.saveMappings(this.mappingsFileChooser.getSelectedFile().toPath());
		} else if (this.mappingsFileChooser.showSaveDialog(this.mainWindow.getFrame()) == JFileChooser.APPROVE_OPTION) {
			return this.controller.saveMappings(ExtensionFileFilter.getSavePath(this.mappingsFileChooser));
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
			}, I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		this.dockerManager.getLeftDock().saveDividerState();
		this.dockerManager.getRightDock().saveDividerState();
		Config.main().windowPos.setValue(Config.Vec2i.fromPoint(this.mainWindow.getFrame().getLocationOnScreen()), true);
		Config.main().windowSize.setValue(Config.Vec2i.fromDimension(this.mainWindow.getFrame().getSize()), true);

		Config.main().save();

		this.searchDialog.dispose();
		this.mainWindow.getFrame().dispose();

		System.exit(0);
	}

	public void redraw() {
		JFrame frame = this.mainWindow.getFrame();

		frame.validate();
		frame.repaint();
	}

	/**
	 * Moves the provided {@link ClassEntry} to the appropriate class selectors for its new state,
	 * and updates stats and swing state accordingly.
	 * @param classEntry the entry to move
	 * @param updateSwingState whether to update swing state (visual reloads)
	 * @param isOldOb whether the class was obfuscated before its name change
	 * @param isNewOb whether the class has become deobfuscated after its name change
	 */
	public void moveClassTree(ClassEntry classEntry, boolean updateSwingState, boolean isOldOb, boolean isNewOb) {
		ClassSelector deobfuscatedClassSelector = this.dockerManager.getDocker(DeobfuscatedClassesDocker.class).getClassSelector();
		ClassSelector obfuscatedClassSelector = this.dockerManager.getDocker(ObfuscatedClassesDocker.class).getClassSelector();

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
		} else {
			// deobfuscated -> deobfuscated
			deobfuscatedClassSelector.moveClassIn(classEntry);
			if (updateSwingState) {
				deobfuscatedClassSelector.reload();
			}
		}

		// all classes selector always needs updating on class rename
		ClassSelector allClassesSelector = this.dockerManager.getDocker(AllClassesDocker.class).getClassSelector();
		List<ClassSelector.StateEntry> expansionState = allClassesSelector.getExpansionState();
		allClassesSelector.moveClassIn(classEntry);

		if (updateSwingState) {
			allClassesSelector.reload();
			allClassesSelector.restoreExpansionState(expansionState);
			deobfuscatedClassSelector.restoreExpansionState(deobfuscatedPanelExpansionState);
			obfuscatedClassSelector.restoreExpansionState(obfuscatedPanelExpansionState);
			this.reloadStats(classEntry, false);
		}
	}

	/**
	 * Reloads stats for the provided class in all selectors.
	 *
	 * @param classEntry the class to reload
	 * @param propagate whether to also reload ancestors of the class
	 */
	public void reloadStats(ClassEntry classEntry, boolean propagate) {
		this.priorReloadStatsCanceler.set(true);
		final AtomicBoolean currentReloadCanceler = new AtomicBoolean(false);
		this.priorReloadStatsCanceler = currentReloadCanceler;

		List<ClassEntry> toUpdate = new ArrayList<>();
		toUpdate.add(classEntry);
		if (propagate) {
			Collection<ClassEntry> parents = this.controller.getProject().getJarIndex().getIndex(InheritanceIndex.class)
					.getAncestors(classEntry);
			toUpdate.addAll(parents);
		}

		final List<Runnable> currentReloads = this.dockerManager.getDockers().stream()
				.flatMap(docker -> docker instanceof ClassesDocker classes ? Stream.of(classes) : Stream.empty())
				.flatMap(docker -> toUpdate.stream().<Runnable>map(updating -> () -> {
					try {
						docker.getClassSelector().reloadStats(updating, currentReloadCanceler::get).get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				}))
				.toList();

		this.reloadStatsExecutor.execute(() -> CompletableFuture
				.allOf(
					currentReloads.stream()
						.map(CompletableFuture::runAsync)
						.toArray(CompletableFuture[]::new)
				)
				.join()
		);
	}

	public SearchDialog getSearchDialog() {
		return this.searchDialog;
	}

	public void addMessage(ServerMessage message) {
		JScrollBar verticalScrollBar = this.dockerManager.getDocker(CollabDocker.class).getMessageScrollPane().getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		this.messageModel.addElement(message);

		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}

		// popup notifications
		switch (message.getType()) {
			case CHAT -> {
				if (Config.main().serverNotificationLevel.value() == NotificationManager.ServerNotificationLevel.FULL && !message.user.equals(Config.net().username.value())) {
					this.notificationManager.notify(new ParameterizedMessage(Message.MULTIPLAYER_CHAT, message.translate()));
				}
			}
			case CONNECT -> {
				if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
					this.notificationManager.notify(new ParameterizedMessage(Message.MULTIPLAYER_USER_CONNECTED, message.translate()));
				}
			}
			case DISCONNECT -> {
				if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
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
		CollabDocker collabDocker = this.dockerManager.getDocker(CollabDocker.class);
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
		this.connectionStatusLabel.setText(I18n.translate("status.prefix") + I18n.translate(this.connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));
	}

	public void retranslateUi() {
		this.jarFileChooser.setDialogTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setDialogTitle(I18n.translate("menu.file.export.jar"));

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.infoPanel.retranslateUi();
		this.editorTabbedPane.retranslateUi();
		for (Docker docker : this.dockerManager.getDockers()) {
			docker.retranslateUi();
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
		Config.RecentProject project = Config.getMostRecentProject();

		if (project != null) {
			this.getNotificationManager().notify(ParameterizedMessage.openedProject(project.jarPath(), project.mappingsPath()));
			this.controller.openJar(project.getJarPath()).whenComplete((v, t) -> this.controller.openMappings(project.getMappingsPath()));
		}
	}
}

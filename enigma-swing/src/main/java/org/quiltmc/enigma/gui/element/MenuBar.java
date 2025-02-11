package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.NotificationManager;
import org.quiltmc.enigma.gui.config.Decompiler;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.AboutDialog;
import org.quiltmc.enigma.gui.dialog.ChangeDialog;
import org.quiltmc.enigma.gui.dialog.ConnectToServerDialog;
import org.quiltmc.enigma.gui.dialog.CrashDialog;
import org.quiltmc.enigma.gui.dialog.CreateServerDialog;
import org.quiltmc.enigma.gui.dialog.FontDialog;
import org.quiltmc.enigma.gui.dialog.SearchDialog;
import org.quiltmc.enigma.gui.dialog.StatsDialog;
import org.quiltmc.enigma.gui.dialog.decompiler.DecompilerSettingsDialog;
import org.quiltmc.enigma.gui.dialog.keybind.ConfigureKeyBindsDialog;
import org.quiltmc.enigma.gui.util.ExtensionFileFilter;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.LanguageUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Pair;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;

import javax.annotation.Nullable;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MenuBar {
	private final JMenu fileMenu = new JMenu();
	private final JMenuItem jarOpenItem = new JMenuItem();
	private final JMenuItem jarCloseItem = new JMenuItem();
	private final JMenuItem openMappingsItem = new JMenuItem();
	private final JMenu openRecentMenu = new JMenu();
	private final JMenuItem maxRecentFilesItem = new JMenuItem();
	private final JMenuItem saveMappingsItem = new JMenuItem();
	private final JMenu saveMappingsAsMenu = new JMenu();
	private final JMenuItem closeMappingsItem = new JMenuItem();
	private final JMenuItem dropMappingsItem = new JMenuItem();
	private final JMenuItem reloadMappingsItem = new JMenuItem();
	private final JMenuItem reloadAllItem = new JMenuItem();
	private final JMenuItem exportSourceItem = new JMenuItem();
	private final JMenuItem exportJarItem = new JMenuItem();
	private final JMenuItem statsItem = new JMenuItem();
	private final JMenuItem configureKeyBindsItem = new JMenuItem();
	private final JMenuItem exitItem = new JMenuItem();
	private final JMenu crashHistoryMenu = new JMenu();

	private final JMenu decompilerMenu = new JMenu();
	private final JMenuItem decompilerSettingsItem = new JMenuItem();

	private final JMenu viewMenu = new JMenu();
	private final JMenu themesMenu = new JMenu();
	private final JMenu languagesMenu = new JMenu();
	private final JMenu scaleMenu = new JMenu();
	private final JMenu notificationsMenu = new JMenu();
	private final JMenu statIconsMenu = new JMenu();
	private final JMenuItem fontItem = new JMenuItem();
	private final JMenuItem customScaleItem = new JMenuItem();

	private final JMenu searchMenu = new JMenu();
	private final JMenuItem searchItem = new JMenuItem(GuiUtil.DEOBFUSCATED_ICON);
	private final JMenuItem searchAllItem = new JMenuItem(GuiUtil.DEOBFUSCATED_ICON);
	private final JMenuItem searchClassItem = new JMenuItem(GuiUtil.CLASS_ICON);
	private final JMenuItem searchMethodItem = new JMenuItem(GuiUtil.METHOD_ICON);
	private final JMenuItem searchFieldItem = new JMenuItem(GuiUtil.FIELD_ICON);

	private final JMenu collabMenu = new JMenu();
	private final JMenuItem connectItem = new JMenuItem();
	private final JMenuItem startServerItem = new JMenuItem();

	private final JMenu helpMenu = new JMenu();
	private final JMenuItem aboutItem = new JMenuItem();
	private final JMenuItem githubItem = new JMenuItem();

	// Enabled with system property "enigma.development" or "--development" flag
	private final DevMenu devMenu;

	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;
		this.devMenu = new DevMenu(gui);

		JMenuBar ui = gui.getMainWindow().getMenuBar();

		this.retranslateUi();

		this.reloadOpenRecentMenu(gui);
		prepareSaveMappingsAsMenu(this.saveMappingsAsMenu, this.saveMappingsItem, gui);
		prepareDecompilerMenu(this.decompilerMenu, this.decompilerSettingsItem, gui);
		prepareThemesMenu(this.themesMenu, gui);
		prepareLanguagesMenu(this.languagesMenu);
		prepareScaleMenu(this.scaleMenu, gui);
		prepareNotificationsMenu(this.notificationsMenu);
		this.prepareStatIconsMenu(this.statIconsMenu);
		this.prepareCrashHistoryMenu();

		this.fileMenu.add(this.jarOpenItem);
		this.fileMenu.add(this.jarCloseItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openRecentMenu);
		this.fileMenu.add(this.maxRecentFilesItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openMappingsItem);
		this.fileMenu.add(this.saveMappingsItem);
		this.fileMenu.add(this.saveMappingsAsMenu);
		this.fileMenu.add(this.closeMappingsItem);
		this.fileMenu.add(this.dropMappingsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.reloadMappingsItem);
		this.fileMenu.add(this.reloadAllItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.exportSourceItem);
		this.fileMenu.add(this.exportJarItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.statsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.configureKeyBindsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.crashHistoryMenu);
		this.fileMenu.add(this.exitItem);
		ui.add(this.fileMenu);

		ui.add(this.decompilerMenu);

		this.viewMenu.add(this.themesMenu);
		this.viewMenu.add(this.languagesMenu);
		this.viewMenu.add(this.notificationsMenu);
		this.scaleMenu.add(this.customScaleItem);
		this.viewMenu.add(this.scaleMenu);
		this.viewMenu.add(this.statIconsMenu);
		this.viewMenu.add(this.fontItem);
		ui.add(this.viewMenu);

		this.searchMenu.add(this.searchItem);
		this.searchMenu.add(this.searchAllItem);
		this.searchMenu.add(this.searchClassItem);
		this.searchMenu.add(this.searchMethodItem);
		this.searchMenu.add(this.searchFieldItem);
		ui.add(this.searchMenu);

		this.collabMenu.add(this.connectItem);
		this.collabMenu.add(this.startServerItem);
		ui.add(this.collabMenu);

		this.helpMenu.add(this.aboutItem);
		this.helpMenu.add(this.githubItem);
		ui.add(this.helpMenu);

		if (System.getProperty("enigma.development", "false").equalsIgnoreCase("true") || Config.main().development.anyEnabled) {
			ui.add(this.devMenu);
		}

		this.setKeyBinds();

		this.jarOpenItem.addActionListener(e -> this.onOpenJarClicked());
		this.openMappingsItem.addActionListener(e -> this.onOpenMappingsClicked());
		this.jarCloseItem.addActionListener(e -> this.gui.getController().closeJar());
		this.maxRecentFilesItem.addActionListener(e -> this.onMaxRecentFilesClicked());
		this.saveMappingsItem.addActionListener(e -> this.onSaveMappingsClicked());
		this.closeMappingsItem.addActionListener(e -> this.onCloseMappingsClicked());
		this.dropMappingsItem.addActionListener(e -> this.gui.getController().dropMappings());
		this.reloadMappingsItem.addActionListener(e -> this.onReloadMappingsClicked());
		this.reloadAllItem.addActionListener(e -> this.onReloadAllClicked());
		this.exportSourceItem.addActionListener(e -> this.onExportSourceClicked());
		this.exportJarItem.addActionListener(e -> this.onExportJarClicked());
		this.statsItem.addActionListener(e -> StatsDialog.show(this.gui));
		this.configureKeyBindsItem.addActionListener(e -> ConfigureKeyBindsDialog.show(this.gui));
		this.exitItem.addActionListener(e -> this.gui.close());
		this.decompilerSettingsItem.addActionListener(e -> DecompilerSettingsDialog.show(this.gui));
		this.customScaleItem.addActionListener(e -> this.onCustomScaleClicked());
		this.fontItem.addActionListener(e -> this.onFontClicked(this.gui));
		this.searchItem.addActionListener(e -> this.onSearchClicked(false));
		this.searchAllItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.values()));
		this.searchClassItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.CLASS));
		this.searchMethodItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.METHOD));
		this.searchFieldItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.FIELD));
		this.connectItem.addActionListener(e -> this.onConnectClicked());
		this.startServerItem.addActionListener(e -> this.onStartServerClicked());
		this.aboutItem.addActionListener(e -> AboutDialog.show(this.gui.getFrame()));
		this.githubItem.addActionListener(e -> this.onGithubClicked());
	}

	public void setKeyBinds() {
		this.saveMappingsItem.setAccelerator(KeyBinds.SAVE_MAPPINGS.toKeyStroke());
		this.dropMappingsItem.setAccelerator(KeyBinds.DROP_MAPPINGS.toKeyStroke());
		this.reloadMappingsItem.setAccelerator(KeyBinds.RELOAD_MAPPINGS.toKeyStroke());
		this.reloadAllItem.setAccelerator(KeyBinds.RELOAD_ALL.toKeyStroke());
		this.statsItem.setAccelerator(KeyBinds.MAPPING_STATS.toKeyStroke());
		this.searchItem.setAccelerator(KeyBinds.SEARCH.toKeyStroke());
		this.searchAllItem.setAccelerator(KeyBinds.SEARCH_ALL.toKeyStroke());
		this.searchClassItem.setAccelerator(KeyBinds.SEARCH_CLASS.toKeyStroke());
		this.searchMethodItem.setAccelerator(KeyBinds.SEARCH_METHOD.toKeyStroke());
		this.searchFieldItem.setAccelerator(KeyBinds.SEARCH_FIELD.toKeyStroke());
	}

	public void updateUiState() {
		boolean jarOpen = this.gui.isJarOpen();
		ConnectionState connectionState = this.gui.getConnectionState();

		this.connectItem.setEnabled(jarOpen && connectionState != ConnectionState.HOSTING);
		this.connectItem.setText(I18n.translate(connectionState != ConnectionState.CONNECTED ? "menu.collab.connect" : "menu.collab.disconnect"));
		this.startServerItem.setEnabled(jarOpen && connectionState != ConnectionState.CONNECTED);
		this.startServerItem.setText(I18n.translate(connectionState != ConnectionState.HOSTING ? "menu.collab.server.start" : "menu.collab.server.stop"));

		this.jarCloseItem.setEnabled(jarOpen);
		this.openMappingsItem.setEnabled(jarOpen);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.mappingsFileChooser.getSelectedFile() != null && connectionState != ConnectionState.CONNECTED);
		this.saveMappingsAsMenu.setEnabled(jarOpen);
		this.closeMappingsItem.setEnabled(jarOpen);
		this.reloadMappingsItem.setEnabled(jarOpen);
		this.reloadAllItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
		this.statsItem.setEnabled(jarOpen);

		this.devMenu.updateUiState();
	}

	public void retranslateUi() {
		this.fileMenu.setText(I18n.translate("menu.file"));
		this.jarOpenItem.setText(I18n.translate("menu.file.jar.open"));
		this.jarCloseItem.setText(I18n.translate("menu.file.jar.close"));
		this.openRecentMenu.setText(I18n.translate("menu.file.open_recent_project"));
		this.maxRecentFilesItem.setText(I18n.translate("menu.file.max_recent_projects"));
		this.openMappingsItem.setText(I18n.translate("menu.file.mappings.open"));
		this.saveMappingsItem.setText(I18n.translate("menu.file.mappings.save"));
		this.saveMappingsAsMenu.setText(I18n.translate("menu.file.mappings.save_as"));
		this.closeMappingsItem.setText(I18n.translate("menu.file.mappings.close"));
		this.dropMappingsItem.setText(I18n.translate("menu.file.mappings.drop"));
		this.reloadMappingsItem.setText(I18n.translate("menu.file.reload_mappings"));
		this.reloadAllItem.setText(I18n.translate("menu.file.reload_all"));
		this.exportSourceItem.setText(I18n.translate("menu.file.export.source"));
		this.exportJarItem.setText(I18n.translate("menu.file.export.jar"));
		this.statsItem.setText(I18n.translate("menu.file.stats"));
		this.configureKeyBindsItem.setText(I18n.translate("menu.file.configure_keybinds"));
		this.crashHistoryMenu.setText(I18n.translate("menu.file.crash_history"));
		this.exitItem.setText(I18n.translate("menu.file.exit"));

		this.decompilerMenu.setText(I18n.translate("menu.decompiler"));
		this.decompilerSettingsItem.setText(I18n.translate("menu.decompiler.settings"));

		this.viewMenu.setText(I18n.translate("menu.view"));
		this.themesMenu.setText(I18n.translate("menu.view.themes"));
		this.notificationsMenu.setText(I18n.translate("menu.view.notifications"));
		this.languagesMenu.setText(I18n.translate("menu.view.languages"));
		this.scaleMenu.setText(I18n.translate("menu.view.scale"));
		this.statIconsMenu.setText(I18n.translate("menu.view.stat_icons"));
		this.fontItem.setText(I18n.translate("menu.view.font"));
		this.customScaleItem.setText(I18n.translate("menu.view.scale.custom"));

		this.searchMenu.setText(I18n.translate("menu.search"));
		this.searchItem.setText(I18n.translate("menu.search"));
		this.searchAllItem.setText(I18n.translate("menu.search.all"));
		this.searchClassItem.setText(I18n.translate("menu.search.class"));
		this.searchMethodItem.setText(I18n.translate("menu.search.method"));
		this.searchFieldItem.setText(I18n.translate("menu.search.field"));

		this.collabMenu.setText(I18n.translate("menu.collab"));
		this.connectItem.setText(I18n.translate("menu.collab.connect"));
		this.startServerItem.setText(I18n.translate("menu.collab.server.start"));

		this.helpMenu.setText(I18n.translate("menu.help"));
		this.aboutItem.setText(I18n.translate("menu.help.about"));
		this.githubItem.setText(I18n.translate("menu.help.github"));

		this.devMenu.retranslateUi();
	}

	private void onOpenJarClicked() {
		JFileChooser d = this.gui.jarFileChooser;
		d.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
		d.setVisible(true);
		int result = d.showOpenDialog(this.gui.getFrame());

		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = d.getSelectedFile();
		// checks if the file name is not empty
		if (file != null) {
			Path path = file.toPath();
			// checks if the file name corresponds to an existing file
			if (Files.exists(path)) {
				this.gui.getController().openJar(path);
			}

			Config.main().stats.lastSelectedDir.setValue(d.getCurrentDirectory().getAbsolutePath(), true);
		}
	}

	private void onMaxRecentFilesClicked() {
		String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.file.dialog.max_recent_projects.set"), Config.main().maxRecentProjects.value());

		if (input != null) {
			try {
				int max = Integer.parseInt(input);
				if (max < 0) {
					throw new NumberFormatException();
				}

				Config.main().maxRecentProjects.setValue(max, true);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate("prompt.invalid_input"), I18n.translate("prompt.error"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void onSaveMappingsClicked() {
		this.gui.getController().saveMappings(this.gui.mappingsFileChooser.getSelectedFile().toPath());
	}

	private void openMappingsDiscardPrompt(Runnable then) {
		if (this.gui.getController().isDirty()) {
			this.gui.showDiscardDiag((response -> {
				if (response == JOptionPane.YES_OPTION) {
					this.gui.saveMapping().thenRun(then);
				} else if (response == JOptionPane.NO_OPTION) {
					then.run();
				}

				return null;
			}), I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		} else {
			then.run();
		}
	}

	private void onCloseMappingsClicked() {
		this.openMappingsDiscardPrompt(() -> this.gui.getController().closeMappings());
	}

	private void onReloadMappingsClicked() {
		this.openMappingsDiscardPrompt(() -> this.gui.getController().reloadMappings());
	}

	private void onReloadAllClicked() {
		this.openMappingsDiscardPrompt(() -> this.gui.getController().reloadAll());
	}

	private void onExportSourceClicked() {
		this.gui.exportSourceFileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
		if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			Config.main().stats.lastSelectedDir.setValue(this.gui.exportSourceFileChooser.getCurrentDirectory().toString(), true);
			this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
		}
	}

	private void onExportJarClicked() {
		this.gui.exportJarFileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
		this.gui.exportJarFileChooser.setVisible(true);
		int result = this.gui.exportJarFileChooser.showSaveDialog(this.gui.getFrame());

		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		if (this.gui.exportJarFileChooser.getSelectedFile() != null) {
			Path path = this.gui.exportJarFileChooser.getSelectedFile().toPath();
			this.gui.getController().exportJar(path);
			Config.main().stats.lastSelectedDir.setValue(this.gui.exportJarFileChooser.getCurrentDirectory().getAbsolutePath(), true);
		}
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
				JOptionPane.QUESTION_MESSAGE, null, null, Double.toString(Config.main().scaleFactor.value() * 100));

		if (answer == null) {
			return;
		}

		float newScale = 1.0f;
		try {
			newScale = Float.parseFloat(answer) / 100f;
		} catch (NumberFormatException ignored) {
			// ignored!
		}

		ScaleUtil.setScaleFactor(newScale);
		ChangeDialog.show(this.gui.getFrame());
	}

	private void onFontClicked(Gui gui) {
		FontDialog.display(gui.getFrame());
	}

	private void onSearchClicked(boolean clear, SearchDialog.Type... types) {
		if (this.gui.getController().getProject() != null) {
			this.gui.getSearchDialog().show(clear, types);
		}
	}

	public void onConnectClicked() {
		if (this.gui.getController().getClient() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}

		ConnectToServerDialog.Result result = ConnectToServerDialog.show(this.gui);
		if (result == null) {
			return;
		}

		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createClient(result.username(), result.address().address, result.address().port, result.password());
			if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.CONNECTED_TO_SERVER, result.addressStr()));
			}

			Config.net().username.setValue(result.username(), true);
			Config.net().remoteAddress.setValue(result.addressStr(), true);
			Config.net().password.setValue(String.valueOf(result.password()), true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.connect.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}

		Arrays.fill(result.password(), (char) 0);
	}

	public void onStartServerClicked() {
		if (this.gui.getController().getServer() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}

		CreateServerDialog.Result result = CreateServerDialog.show(this.gui);
		if (result == null) {
			return;
		}

		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createServer(result.username(), result.port(), result.password());
			if (Config.main().serverNotificationLevel.value() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.SERVER_STARTED, result.port()));
			}

			Config.net().username.setValue(result.username(), true);
			Config.net().serverPort.setValue(result.port(), true);
			Config.net().serverPassword.setValue(String.valueOf(result.password()), true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.server.start.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
	}

	private void onGithubClicked() {
		GuiUtil.openUrl("https://github.com/QuiltMC/Enigma");
	}

	private void onOpenMappingsClicked() {
		this.gui.mappingsFileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
		this.gui.mappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		List<ReadWriteService> types = this.gui.getController().getEnigma().getReadWriteServices().stream().filter(ReadWriteService::supportsReading).toList();
		ExtensionFileFilter.setupFileChooser(this.gui, this.gui.mappingsFileChooser, types.toArray(new ReadWriteService[0]));

		if (this.gui.mappingsFileChooser.showOpenDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			File selectedFile = this.gui.mappingsFileChooser.getSelectedFile();
			Config.main().stats.lastSelectedDir.setValue(this.gui.mappingsFileChooser.getCurrentDirectory().toString(), true);

			Optional<ReadWriteService> format = this.gui.getController().getEnigma().getReadWriteService(selectedFile.toPath());
			if (format.isPresent() && format.get().supportsReading()) {
				this.gui.getController().openMappings(format.get(), selectedFile.toPath());
			} else {
				String nonParseableMessage = I18n.translateFormatted("menu.file.open.non_parseable.unsupported_format", selectedFile);
				if (format.isPresent()) {
					nonParseableMessage = I18n.translateFormatted("menu.file.open.non_parseable", I18n.translate("mapping_format." + format.get().getId().split(":")[1].toLowerCase()));
				}

				JOptionPane.showMessageDialog(this.gui.getFrame(), nonParseableMessage, I18n.translate("menu.file.open.cannot_open"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void reloadOpenRecentMenu(Gui gui) {
		this.openRecentMenu.removeAll();
		List<Config.RecentProject> recentFilePairs = Config.main().recentProjects.value();

		// find the longest common prefix among all mappings files
		// this is to clear the "/home/user/wherever-you-store-your-mappings-projects/" part of the path and only show relevant information
		Path prefix = null;

		if (recentFilePairs.size() > 1) {
			List<Path> recentFiles = recentFilePairs.stream().map(Config.RecentProject::getMappingsPath).sorted().toList();
			prefix = recentFiles.get(0);

			for (int i = 1; i < recentFiles.size(); i++) {
				if (prefix == null) {
					break;
				}

				prefix = findCommonPath(prefix, recentFiles.get(i));
			}
		}

		for (Config.RecentProject recent : recentFilePairs) {
			if (!Files.exists(recent.getJarPath()) || !Files.exists(recent.getMappingsPath())) {
				continue;
			}

			String jarName = recent.getJarPath().getFileName().toString();

			// if there's no common prefix, just show the last directory in the tree
			String mappingsName;
			if (prefix != null) {
				mappingsName = prefix.relativize(recent.getMappingsPath()).toString();
			} else {
				mappingsName = recent.getMappingsPath().getFileName().toString();
			}

			JMenuItem item = new JMenuItem(jarName + " -> " + mappingsName);
			item.addActionListener(event -> gui.getController().openJar(recent.getJarPath()).whenComplete((v, t) -> gui.getController().openMappings(recent.getMappingsPath())));
			this.openRecentMenu.add(item);
		}
	}

	/**
	 * Find the longest common path between two absolute(!!) paths.
	 */
	@Nullable
	private static Path findCommonPath(Path a, Path b) {
		int i = 0;
		for (; i < Math.min(a.getNameCount(), b.getNameCount()); i++) {
			Path nameA = a.getName(i);
			Path nameB = b.getName(i);

			if (!nameA.equals(nameB)) {
				break;
			}
		}

		return i != 0 ? a.getRoot().resolve(a.subpath(0, i)) : null;
	}

	private static void prepareSaveMappingsAsMenu(JMenu saveMappingsAsMenu, JMenuItem saveMappingsItem, Gui gui) {
		for (ReadWriteService format : gui.getController().getEnigma().getReadWriteServices()) {
			if (format.supportsWriting()) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.getId().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					JFileChooser fileChooser = gui.mappingsFileChooser;
					ExtensionFileFilter.setupFileChooser(gui, fileChooser, format);

					if (fileChooser.getCurrentDirectory() == null) {
						fileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
					}

					if (fileChooser.showSaveDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						Path savePath = ExtensionFileFilter.getSavePath(fileChooser);
						gui.getController().saveMappings(savePath, format);
						saveMappingsItem.setEnabled(true);
						Config.main().stats.lastSelectedDir.setValue(fileChooser.getCurrentDirectory().toString());
					}
				});
				saveMappingsAsMenu.add(item);
			}
		}
	}

	private static void prepareDecompilerMenu(JMenu decompilerMenu, JMenuItem decompilerSettingsItem, Gui gui) {
		ButtonGroup decompilerGroup = new ButtonGroup();

		for (Decompiler decompiler : Decompiler.values()) {
			JRadioButtonMenuItem decompilerButton = new JRadioButtonMenuItem(decompiler.name);
			decompilerGroup.add(decompilerButton);
			if (decompiler.equals(Config.decompiler().activeDecompiler.value())) {
				decompilerButton.setSelected(true);
			}

			decompilerButton.addActionListener(event -> {
				gui.getController().setDecompiler(decompiler.service);

				Config.decompiler().activeDecompiler.setValue(decompiler, true);
			});
			decompilerMenu.add(decompilerButton);
		}

		decompilerMenu.addSeparator();
		decompilerMenu.add(decompilerSettingsItem);
	}

	private static void prepareThemesMenu(JMenu themesMenu, Gui gui) {
		ButtonGroup themeGroup = new ButtonGroup();
		for (Config.ThemeChoice themeChoice : Config.ThemeChoice.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + themeChoice.name().toLowerCase(Locale.ROOT)));
			themeGroup.add(themeButton);
			if (themeChoice.equals(Config.main().theme.value())) {
				themeButton.setSelected(true);
			}

			themeButton.addActionListener(e -> {
				Config.main().theme.setValue(themeChoice, true);
				ChangeDialog.show(gui.getFrame());
			});
			themesMenu.add(themeButton);
		}
	}

	private static void prepareLanguagesMenu(JMenu languagesMenu) {
		ButtonGroup languageGroup = new ButtonGroup();
		for (String lang : I18n.getAvailableLanguages()) {
			JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
			languageGroup.add(languageButton);
			if (lang.equals(Config.main().language.value())) {
				languageButton.setSelected(true);
			}

			languageButton.addActionListener(event -> {
				Config.main().language.setValue(lang, true);
				I18n.setLanguage(lang);
				LanguageUtil.dispatchLanguageChange();
			});
			languagesMenu.add(languageButton);
		}
	}

	private static void prepareScaleMenu(JMenu scaleMenu, Gui gui) {
		ButtonGroup scaleGroup = new ButtonGroup();
		Map<Float, JRadioButtonMenuItem> scaleButtons = IntStream.of(100, 125, 150, 175, 200)
				.mapToObj(scaleFactor -> {
					float realScaleFactor = scaleFactor / 100f;
					JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(String.format("%d%%", scaleFactor));
					menuItem.addActionListener(event -> ScaleUtil.setScaleFactor(realScaleFactor));
					menuItem.addActionListener(event -> ChangeDialog.show(gui.getFrame()));
					scaleGroup.add(menuItem);
					scaleMenu.add(menuItem);
					return new Pair<>(realScaleFactor, menuItem);
				})
				.collect(Collectors.toMap(Pair::a, Pair::b));

		JRadioButtonMenuItem currentScaleButton = scaleButtons.get(Config.main().scaleFactor.value());
		if (currentScaleButton != null) {
			currentScaleButton.setSelected(true);
		}

		ScaleUtil.addListener((newScale, oldScale) -> {
			JRadioButtonMenuItem mi = scaleButtons.get(newScale);
			if (mi != null) {
				mi.setSelected(true);
			} else {
				scaleGroup.clearSelection();
			}
		});
	}

	private static void prepareNotificationsMenu(JMenu notificationsMenu) {
		ButtonGroup notificationsGroup = new ButtonGroup();

		for (NotificationManager.ServerNotificationLevel level : NotificationManager.ServerNotificationLevel.values()) {
			JRadioButtonMenuItem notificationsButton = new JRadioButtonMenuItem(level.getText());
			notificationsGroup.add(notificationsButton);

			if (level.equals(Config.main().serverNotificationLevel.value())) {
				notificationsButton.setSelected(true);
			}

			notificationsButton.addActionListener(event -> Config.main().serverNotificationLevel.setValue(level, true));

			notificationsMenu.add(notificationsButton);
		}
	}

	private void prepareStatIconsMenu(JMenu statIconsMenu) {
		JMenu statTypes = new JMenu(I18n.translate("menu.view.stat_icons.included_types"));
		for (StatType statType : StatType.values()) {
			JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(statType.getName());
			checkbox.setSelected(Config.main().stats.includedStatTypes.value().contains(statType));
			checkbox.addActionListener(event -> {
				if (checkbox.isSelected() && !Config.stats().includedStatTypes.value().contains(statType)) {
					Config.stats().includedStatTypes.value().add(statType);
				} else {
					Config.stats().includedStatTypes.value().remove(statType);
				}

				MenuBar.this.gui.getController().regenerateAndUpdateStatIcons();
			});

			statTypes.add(checkbox);
		}

		JCheckBoxMenuItem enableIcons = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.enable_icons"));
		JCheckBoxMenuItem includeSynthetic = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.include_synthetic"));
		JCheckBoxMenuItem countFallback = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.count_fallback"));

		enableIcons.setSelected(Config.main().features.enableClassTreeStatIcons.value());
		includeSynthetic.setSelected(Config.main().stats.shouldIncludeSyntheticParameters.value());
		countFallback.setSelected(Config.main().stats.shouldCountFallbackNames.value());

		enableIcons.addActionListener(event -> {
			Config.main().features.enableClassTreeStatIcons.setValue(enableIcons.isSelected());
			MenuBar.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		includeSynthetic.addActionListener(event -> {
			Config.main().stats.shouldIncludeSyntheticParameters.setValue(includeSynthetic.isSelected());
			MenuBar.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		countFallback.addActionListener(event -> {
			Config.main().stats.shouldCountFallbackNames.setValue(countFallback.isSelected());
			MenuBar.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		statIconsMenu.add(enableIcons);
		statIconsMenu.add(includeSynthetic);
		statIconsMenu.add(countFallback);
		statIconsMenu.add(statTypes);
	}

	public void prepareCrashHistoryMenu() {
		this.crashHistoryMenu.removeAll();
		ButtonGroup crashHistoryGroup = new ButtonGroup();

		for (int i = 0; i < this.gui.getCrashHistory().size(); i++) {
			Throwable t = this.gui.getCrashHistory().get(i);
			JMenuItem crashHistoryButton = new JMenuItem(i + " - " + t.toString());
			crashHistoryGroup.add(crashHistoryButton);

			crashHistoryButton.addActionListener(event -> CrashDialog.show(t, false));

			this.crashHistoryMenu.add(crashHistoryButton);
		}

		this.crashHistoryMenu.setEnabled(!this.gui.getCrashHistory().isEmpty());
	}
}

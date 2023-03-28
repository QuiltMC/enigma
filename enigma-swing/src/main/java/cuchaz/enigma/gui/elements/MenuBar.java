package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ConnectionState;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.NotificationManager;
import cuchaz.enigma.gui.config.Decompiler;
import cuchaz.enigma.gui.config.LookAndFeel;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.gui.dialog.ChangeDialog;
import cuchaz.enigma.gui.dialog.ConnectToServerDialog;
import cuchaz.enigma.gui.dialog.CreateServerDialog;
import cuchaz.enigma.gui.dialog.FontDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.dialog.StatsDialog;
import cuchaz.enigma.gui.dialog.decompiler.DecompilerSettingsDialog;
import cuchaz.enigma.gui.dialog.keybind.ConfigureKeyBindsDialog;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ParameterizedMessage;

import javax.annotation.Nullable;
import javax.swing.ButtonGroup;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MenuBar {
	private final JMenu fileMenu = new JMenu();
	private final JMenuItem jarOpenItem = new JMenuItem();
	private final JMenuItem jarCloseItem = new JMenuItem();
	private final JMenu openMenu = new JMenu();
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

	private final JMenu decompilerMenu = new JMenu();
	private final JMenuItem decompilerSettingsItem = new JMenuItem();

	private final JMenu viewMenu = new JMenu();
	private final JMenu themesMenu = new JMenu();
	private final JMenu languagesMenu = new JMenu();
	private final JMenu scaleMenu = new JMenu();
	private final JMenu notificationsMenu = new JMenu();
	private final JMenuItem fontItem = new JMenuItem();
	private final JMenuItem customScaleItem = new JMenuItem();

	private final JMenu searchMenu = new JMenu();
	private final JMenuItem searchClassItem = new JMenuItem(GuiUtil.CLASS_ICON);
	private final JMenuItem searchMethodItem = new JMenuItem(GuiUtil.METHOD_ICON);
	private final JMenuItem searchFieldItem = new JMenuItem(GuiUtil.FIELD_ICON);

	private final JMenu collabMenu = new JMenu();
	private final JMenuItem connectItem = new JMenuItem();
	private final JMenuItem startServerItem = new JMenuItem();

	private final JMenu helpMenu = new JMenu();
	private final JMenuItem aboutItem = new JMenuItem();
	private final JMenuItem githubItem = new JMenuItem();

	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		JMenuBar ui = gui.getMainWindow().getMenuBar();

		this.retranslateUi();

		prepareOpenMenu(this.openMenu, gui);
		this.reloadOpenRecentMenu(gui);
		prepareSaveMappingsAsMenu(this.saveMappingsAsMenu, this.saveMappingsItem, gui);
		prepareDecompilerMenu(this.decompilerMenu, this.decompilerSettingsItem, gui);
		prepareThemesMenu(this.themesMenu, gui);
		prepareLanguagesMenu(this.languagesMenu);
		prepareScaleMenu(this.scaleMenu, gui);
		prepareNotificationsMenu(this.notificationsMenu);

		this.fileMenu.add(this.jarOpenItem);
		this.fileMenu.add(this.jarCloseItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openRecentMenu);
		this.fileMenu.add(this.maxRecentFilesItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openMenu);
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
		this.fileMenu.add(this.exitItem);
		ui.add(this.fileMenu);

		ui.add(this.decompilerMenu);

		this.viewMenu.add(this.themesMenu);
		this.viewMenu.add(this.languagesMenu);
		this.viewMenu.add(this.notificationsMenu);
		this.scaleMenu.add(this.customScaleItem);
		this.viewMenu.add(this.scaleMenu);
		this.viewMenu.add(this.fontItem);
		ui.add(this.viewMenu);

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

		this.setKeyBinds();

		this.jarOpenItem.addActionListener(e -> this.onOpenJarClicked());
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
		this.searchClassItem.addActionListener(e -> this.onSearchClicked(SearchDialog.Type.CLASS));
		this.searchMethodItem.addActionListener(e -> this.onSearchClicked(SearchDialog.Type.METHOD));
		this.searchFieldItem.addActionListener(e -> this.onSearchClicked(SearchDialog.Type.FIELD));
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
		this.openMenu.setEnabled(jarOpen);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.enigmaMappingsFileChooser.getSelectedFile() != null && connectionState != ConnectionState.CONNECTED);
		this.saveMappingsAsMenu.setEnabled(jarOpen);
		this.closeMappingsItem.setEnabled(jarOpen);
		this.reloadMappingsItem.setEnabled(jarOpen);
		this.reloadAllItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
		this.statsItem.setEnabled(jarOpen);
	}

	public void retranslateUi() {
		this.fileMenu.setText(I18n.translate("menu.file"));
		this.jarOpenItem.setText(I18n.translate("menu.file.jar.open"));
		this.jarCloseItem.setText(I18n.translate("menu.file.jar.close"));
		this.openRecentMenu.setText(I18n.translate("menu.file.open_recent_project"));
		this.maxRecentFilesItem.setText(I18n.translate("menu.file.max_recent_projects"));
		this.openMenu.setText(I18n.translate("menu.file.mappings.open"));
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
		this.exitItem.setText(I18n.translate("menu.file.exit"));

		this.decompilerMenu.setText(I18n.translate("menu.decompiler"));
		this.decompilerSettingsItem.setText(I18n.translate("menu.decompiler.settings"));

		this.viewMenu.setText(I18n.translate("menu.view"));
		this.themesMenu.setText(I18n.translate("menu.view.themes"));
		this.notificationsMenu.setText(I18n.translate("menu.view.notifications"));
		this.languagesMenu.setText(I18n.translate("menu.view.languages"));
		this.scaleMenu.setText(I18n.translate("menu.view.scale"));
		this.fontItem.setText(I18n.translate("menu.view.font"));
		this.customScaleItem.setText(I18n.translate("menu.view.scale.custom"));

		this.searchMenu.setText(I18n.translate("menu.search"));
		this.searchClassItem.setText(I18n.translate("menu.search.class"));
		this.searchMethodItem.setText(I18n.translate("menu.search.method"));
		this.searchFieldItem.setText(I18n.translate("menu.search.field"));

		this.collabMenu.setText(I18n.translate("menu.collab"));
		this.connectItem.setText(I18n.translate("menu.collab.connect"));
		this.startServerItem.setText(I18n.translate("menu.collab.server.start"));

		this.helpMenu.setText(I18n.translate("menu.help"));
		this.aboutItem.setText(I18n.translate("menu.help.about"));
		this.githubItem.setText(I18n.translate("menu.help.github"));
	}

	private void onOpenJarClicked() {
		JFileChooser d = this.gui.jarFileChooser;
		d.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
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

			UiConfig.setLastSelectedDir(d.getCurrentDirectory().getAbsolutePath());
		}
	}

	private void onMaxRecentFilesClicked() {
		String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.file.dialog.max_recent_projects.set"), UiConfig.getMaxRecentFiles());

		if (input != null) {
			try {
				int max = Integer.parseInt(input);
				if (max < 0) {
					throw new NumberFormatException();
				}

				UiConfig.setMaxRecentFiles(max);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate("menu.file.dialog.max_recent_projects.invalid"), I18n.translate("menu.file.dialog.error"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void onSaveMappingsClicked() {
		this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
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
		this.gui.exportSourceFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
		if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			UiConfig.setLastSelectedDir(this.gui.exportSourceFileChooser.getCurrentDirectory().toString());
			this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
		}
	}

	private void onExportJarClicked() {
		this.gui.exportJarFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
		this.gui.exportJarFileChooser.setVisible(true);
		int result = this.gui.exportJarFileChooser.showSaveDialog(this.gui.getFrame());

		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		if (this.gui.exportJarFileChooser.getSelectedFile() != null) {
			Path path = this.gui.exportJarFileChooser.getSelectedFile().toPath();
			this.gui.getController().exportJar(path);
			UiConfig.setLastSelectedDir(this.gui.exportJarFileChooser.getCurrentDirectory().getAbsolutePath());
		}
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
				JOptionPane.QUESTION_MESSAGE, null, null, Float.toString(UiConfig.getScaleFactor() * 100));

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

	private void onSearchClicked(SearchDialog.Type type) {
		if (this.gui.getController().project != null) {
			this.gui.getSearchDialog().show(type);
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
			if (UiConfig.getServerNotificationLevel() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.CONNECTED_TO_SERVER, result.addressStr()));
			}

			NetConfig.setUsername(result.username());
			NetConfig.setRemoteAddress(result.addressStr());
			NetConfig.setPassword(String.valueOf(result.password()));
			NetConfig.save();
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
			this.gui.getController().createServer(result.port(), result.password());
			if (UiConfig.getServerNotificationLevel() != NotificationManager.ServerNotificationLevel.NONE) {
				this.gui.getNotificationManager().notify(new ParameterizedMessage(Message.SERVER_STARTED, result.port()));
			}

			NetConfig.setServerPort(result.port());
			NetConfig.setServerPassword(String.valueOf(result.password()));
			NetConfig.save();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.server.start.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
	}

	private void onGithubClicked() {
		GuiUtil.openUrl("https://github.com/QuiltMC/Enigma");
	}

	private static void prepareOpenMenu(JMenu openMenu, Gui gui) {
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getReader() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					gui.enigmaMappingsFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
					if (gui.enigmaMappingsFileChooser.showOpenDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						File selectedFile = gui.enigmaMappingsFileChooser.getSelectedFile();
						gui.getController().openMappings(format, selectedFile.toPath());
						UiConfig.setLastSelectedDir(gui.enigmaMappingsFileChooser.getCurrentDirectory().toString());
					}
				});
				openMenu.add(item);
			}
		}
	}

	public void reloadOpenRecentMenu(Gui gui) {
		this.openRecentMenu.removeAll();
		List<Pair<Path, Path>> recentFilePairs = UiConfig.getRecentFilePairs();

		// find the longest common prefix among all mappings files
		// this is to clear the "/home/user/wherever-you-store-your-mappings-projects/" part of the path and only show relevant information
		Path prefix = null;

		if (recentFilePairs.size() > 1) {
			List<Path> recentFiles = recentFilePairs.stream().map(Pair::b).sorted().toList();
			prefix = recentFiles.get(0);

			for (int i = 1; i < recentFiles.size(); i++) {
				if (prefix == null) {
					break;
				}

				prefix = findCommonPath(prefix, recentFiles.get(i));
			}
		}

		for (Pair<Path, Path> recent : recentFilePairs) {
			if (!Files.exists(recent.a()) || !Files.exists(recent.b())) {
				continue;
			}

			String jarName = recent.a().getFileName().toString();

			// if there's no common prefix, just show the last directory in the tree
			String mappingsName;
			if (prefix != null) {
				mappingsName = prefix.relativize(recent.b()).toString();
			} else {
				mappingsName = recent.b().getFileName().toString();
			}

			JMenuItem item = new JMenuItem(jarName + " -> " + mappingsName);
			item.addActionListener(event -> gui.getController().openJar(recent.a()).whenComplete((v, t) -> gui.getController().openMappings(recent.b())));
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
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getWriter() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					// TODO: Use a specific file chooser for it
					if (gui.enigmaMappingsFileChooser.getCurrentDirectory() == null) {
						gui.enigmaMappingsFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
					}

					if (gui.enigmaMappingsFileChooser.showSaveDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						gui.getController().saveMappings(gui.enigmaMappingsFileChooser.getSelectedFile().toPath(), format);
						saveMappingsItem.setEnabled(true);
						UiConfig.setLastSelectedDir(gui.enigmaMappingsFileChooser.getCurrentDirectory().toString());
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
			if (decompiler.equals(UiConfig.getDecompiler())) {
				decompilerButton.setSelected(true);
			}

			decompilerButton.addActionListener(event -> {
				gui.getController().setDecompiler(decompiler.service);

				UiConfig.setDecompiler(decompiler);
				UiConfig.save();
			});
			decompilerMenu.add(decompilerButton);
		}

		decompilerMenu.addSeparator();
		decompilerMenu.add(decompilerSettingsItem);
	}

	private static void prepareThemesMenu(JMenu themesMenu, Gui gui) {
		ButtonGroup themeGroup = new ButtonGroup();
		for (LookAndFeel lookAndFeel : LookAndFeel.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + lookAndFeel.name().toLowerCase(Locale.ROOT)));
			themeGroup.add(themeButton);
			if (lookAndFeel.equals(UiConfig.getLookAndFeel())) {
				themeButton.setSelected(true);
			}

			themeButton.addActionListener(e -> {
				UiConfig.setLookAndFeel(lookAndFeel);
				UiConfig.save();
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
			if (lang.equals(UiConfig.getLanguage())) {
				languageButton.setSelected(true);
			}

			languageButton.addActionListener(event -> {
				UiConfig.setLanguage(lang);
				I18n.setLanguage(lang);
				LanguageUtil.dispatchLanguageChange();
				UiConfig.save();
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

		JRadioButtonMenuItem currentScaleButton = scaleButtons.get(UiConfig.getScaleFactor());
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

			if (level.equals(UiConfig.getServerNotificationLevel())) {
				notificationsButton.setSelected(true);
			}

			notificationsButton.addActionListener(event -> UiConfig.setServerNotificationLevel(level));

			notificationsMenu.add(notificationsButton);
		}
	}
}

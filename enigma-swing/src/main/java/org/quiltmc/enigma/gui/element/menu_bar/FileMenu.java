package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.CrashDialog;
import org.quiltmc.enigma.gui.dialog.StatsDialog;
import org.quiltmc.enigma.gui.dialog.keybind.ConfigureKeyBindsDialog;
import org.quiltmc.enigma.gui.util.ExtensionFileFilter;
import org.quiltmc.enigma.util.I18n;

import javax.annotation.Nullable;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FileMenu extends AbstractEnigmaMenu {
	private final Gui gui;

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

	public FileMenu(Gui gui) {
		this.gui = gui;

		this.reloadOpenRecentMenu();
		this.prepareSaveMappingsAsMenu();
		this.prepareCrashHistoryMenu();

		this.add(this.jarOpenItem);
		this.add(this.jarCloseItem);
		this.addSeparator();
		this.add(this.openRecentMenu);
		this.add(this.maxRecentFilesItem);
		this.addSeparator();
		this.add(this.openMappingsItem);
		this.add(this.saveMappingsItem);
		this.add(this.saveMappingsAsMenu);
		this.add(this.closeMappingsItem);
		this.add(this.dropMappingsItem);
		this.addSeparator();
		this.add(this.reloadMappingsItem);
		this.add(this.reloadAllItem);
		this.addSeparator();
		this.add(this.exportSourceItem);
		this.add(this.exportJarItem);
		this.addSeparator();
		this.add(this.statsItem);
		this.addSeparator();
		this.add(this.configureKeyBindsItem);
		this.addSeparator();
		this.add(this.crashHistoryMenu);
		this.add(this.exitItem);

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
	}

	@Override
	public void setKeyBinds() {
		this.saveMappingsItem.setAccelerator(KeyBinds.SAVE_MAPPINGS.toKeyStroke());
		this.dropMappingsItem.setAccelerator(KeyBinds.DROP_MAPPINGS.toKeyStroke());
		this.reloadMappingsItem.setAccelerator(KeyBinds.RELOAD_MAPPINGS.toKeyStroke());
		this.reloadAllItem.setAccelerator(KeyBinds.RELOAD_ALL.toKeyStroke());
		this.statsItem.setAccelerator(KeyBinds.MAPPING_STATS.toKeyStroke());
	}

	@Override
	public void updateState() {
		boolean jarOpen = this.gui.isJarOpen();

		this.jarCloseItem.setEnabled(jarOpen);
		this.openMappingsItem.setEnabled(jarOpen);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.mappingsFileChooser.getSelectedFile() != null && this.gui.getConnectionState() != ConnectionState.CONNECTED);
		this.saveMappingsAsMenu.setEnabled(jarOpen);
		this.closeMappingsItem.setEnabled(jarOpen);
		this.reloadMappingsItem.setEnabled(jarOpen);
		this.reloadAllItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
		this.statsItem.setEnabled(jarOpen);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.file"));
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
	}

	public void reloadOpenRecentMenu() {
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
			item.addActionListener(event -> this.gui.getController().openJar(recent.getJarPath()).whenComplete((v, t) -> this.gui.getController().openMappings(recent.getMappingsPath())));
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

	private void prepareSaveMappingsAsMenu() {
		for (ReadWriteService format : this.gui.getController().getEnigma().getReadWriteServices()) {
			if (format.supportsWriting()) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.getId().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					JFileChooser fileChooser = this.gui.mappingsFileChooser;
					ExtensionFileFilter.setupFileChooser(this.gui, fileChooser, format);

					if (fileChooser.getCurrentDirectory() == null) {
						fileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
					}

					if (fileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						Path savePath = ExtensionFileFilter.getSavePath(fileChooser);
						this.gui.getController().saveMappings(savePath, format);
						this.saveMappingsItem.setEnabled(true);
						Config.main().stats.lastSelectedDir.setValue(fileChooser.getCurrentDirectory().toString());
					}
				});

				this.saveMappingsAsMenu.add(item);
			}
		}
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
}

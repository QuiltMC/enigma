package org.quiltmc.enigma.gui.element.menu_bar.file;

import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.StatsDialog;
import org.quiltmc.enigma.gui.dialog.keybind.ConfigureKeyBindsDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleCheckBoxItem;
import org.quiltmc.enigma.gui.element.menu_bar.SimpleItem;
import org.quiltmc.enigma.gui.util.ExtensionFileFilter;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class FileMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.file";

	private final SaveMappingsAsMenu saveMappingsAs;
	private final CrashHistoryMenu crashHistory;
	private final OpenRecentMenu openRecent;

	private final SimpleItem jarOpenItem = new SimpleItem("menu.file.jar.open");
	private final SimpleItem jarCloseItem = new SimpleItem("menu.file.jar.close");
	private final SimpleItem openMappingsItem = new SimpleItem("menu.file.mappings.open");
	private final SimpleItem maxRecentFilesItem = new SimpleItem("menu.file.max_recent_projects");
	private final SimpleItem saveMappingsItem = new SimpleItem("menu.file.mappings.save");
	private final SimpleCheckBoxItem autoSaveMappingsItem = new SimpleCheckBoxItem("menu.file.mappings.auto_save");
	private final SimpleItem closeMappingsItem = new SimpleItem("menu.file.mappings.close");
	private final SimpleItem dropMappingsItem = new SimpleItem("menu.file.mappings.drop");
	private final SimpleItem reloadMappingsItem = new SimpleItem("menu.file.reload_mappings");
	private final SimpleItem reloadAllItem = new SimpleItem("menu.file.reload_all");
	private final SimpleItem exportSourceItem = new SimpleItem("menu.file.export.source");
	private final SimpleItem exportJarItem = new SimpleItem("menu.file.export.jar");
	private final SimpleItem statsItem = new SimpleItem("menu.file.stats");
	private final SimpleItem configureKeyBindsItem = new SimpleItem("menu.file.configure_keybinds");
	private final SimpleItem exitItem = new SimpleItem("menu.file.exit");

	public FileMenu(Gui gui) {
		super(gui);

		this.saveMappingsAs = new SaveMappingsAsMenu(gui);
		this.crashHistory = new CrashHistoryMenu(gui);
		this.openRecent = new OpenRecentMenu(gui);

		this.add(this.jarOpenItem);
		this.add(this.jarCloseItem);
		this.addSeparator();
		this.add(this.openRecent);
		this.add(this.maxRecentFilesItem);
		this.addSeparator();
		this.add(this.openMappingsItem);
		this.add(this.saveMappingsItem);
		this.add(this.saveMappingsAs);
		this.add(this.autoSaveMappingsItem);
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
		this.add(this.crashHistory);
		this.add(this.exitItem);

		this.jarOpenItem.addActionListener(e -> this.onOpenJarClicked());
		this.openMappingsItem.addActionListener(e -> this.onOpenMappingsClicked());
		this.jarCloseItem.addActionListener(e -> this.gui.getController().closeJar());
		this.maxRecentFilesItem.addActionListener(e -> this.onMaxRecentFilesClicked());
		this.saveMappingsItem.addActionListener(e -> this.onSaveMappingsClicked());
		this.autoSaveMappingsItem.addActionListener(e -> Config.editor().autoSaveMappings.setValue(this.autoSaveMappingsItem.getState()));
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
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.jarCloseItem.setEnabled(jarOpen);
		this.openMappingsItem.setEnabled(jarOpen);
		this.openRecent.updateState(jarOpen, state);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.mappingsFileChooser.getSelectedFile() != null && this.gui.getConnectionState() != ConnectionState.CONNECTED);
		this.saveMappingsAs.updateState();
		this.autoSaveMappingsItem.setEnabled(jarOpen);
		this.autoSaveMappingsItem.setState(Config.editor().autoSaveMappings.value());
		this.closeMappingsItem.setEnabled(jarOpen);
		this.reloadMappingsItem.setEnabled(jarOpen);
		this.reloadAllItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
		this.statsItem.setEnabled(jarOpen);
		this.crashHistory.updateState(jarOpen, state);
		this.openRecent.updateState(jarOpen, state);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.jarOpenItem.retranslate();
		this.jarCloseItem.retranslate();
		this.openRecent.retranslate();
		this.openMappingsItem.retranslate();
		this.maxRecentFilesItem.retranslate();
		this.saveMappingsItem.retranslate();
		this.saveMappingsAs.retranslate();
		this.autoSaveMappingsItem.retranslate();
		this.closeMappingsItem.retranslate();
		this.dropMappingsItem.retranslate();
		this.reloadMappingsItem.retranslate();
		this.reloadAllItem.retranslate();
		this.exportSourceItem.retranslate();
		this.exportJarItem.retranslate();
		this.statsItem.retranslate();
		this.configureKeyBindsItem.retranslate();
		this.crashHistory.retranslate();
		this.exitItem.retranslate();
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
				String nonParseableMessage = format.isPresent()
						? I18n.translateFormatted("menu.file.open.non_parseable", I18n.translate(format.get().getId()))
						: I18n.translateFormatted("menu.file.open.non_parseable.unsupported_format", selectedFile);

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

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

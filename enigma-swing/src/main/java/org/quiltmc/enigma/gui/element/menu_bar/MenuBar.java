package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.file.FileMenu;
import org.quiltmc.enigma.gui.element.menu_bar.view.ViewMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MenuBar {
	private final List<EnigmaMenu> menus = new ArrayList<>();

	private final CollabMenu collabMenu;
	private final FileMenu fileMenu;
	private final HelpMenu helpMenu;

	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		this.fileMenu = new FileMenu(gui);
		DecompilerMenu decompilerMenu = new DecompilerMenu(gui);
		ViewMenu viewMenu = new ViewMenu(gui);
		SearchMenu searchMenu = new SearchMenu(gui);
		this.collabMenu = new CollabMenu(gui);
		this.helpMenu = new HelpMenu(gui);
		// Enabled with system property "enigma.development" or "--development" flag
		DevMenu devMenu = new DevMenu(gui);

		this.retranslateUi();

		this.addMenu(this.fileMenu);
		this.addMenu(decompilerMenu);
		this.addMenu(viewMenu);
		this.addMenu(searchMenu);
		this.addMenu(this.collabMenu);
		this.addMenu(this.helpMenu);

		if (Boolean.parseBoolean(System.getProperty("enigma.development")) || Config.main().development.anyEnabled) {
			this.addMenu(devMenu);
		}

		this.setKeyBinds();
	}

	private void addMenu(AbstractEnigmaMenu menu) {
		this.gui.getMainWindow().getMenuBar().add(menu);
		this.menus.add(menu);
	}

	public void setKeyBinds() {
		for (EnigmaMenu menu : this.menus) {
			menu.setKeyBinds();
		}
	}

	public void updateUiState() {
		boolean jarOpen = this.gui.isJarOpen();
		ConnectionState connectionState = this.gui.getConnectionState();
		for (EnigmaMenu menu : this.menus) {
			menu.updateState(jarOpen, connectionState);
		}

		this.clearSearchMenusResults();
	}

	public void retranslateUi() {
		for (EnigmaMenu menu : this.menus) {
			menu.retranslate();
		}

		this.clearSearchMenusResults();
	}

	public void clearSearchMenusResults() {
		this.helpMenu.clearSearchMenusResults();
	}

	public CollabMenu getCollabMenu() {
		return this.collabMenu;
	}

	public FileMenu getFileMenu() {
		return this.fileMenu;
	}

	public Stream<EnigmaMenu> streamMenus() {
		return this.menus.stream();
	}
}

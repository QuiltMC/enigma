package org.quiltmc.enigma.gui.element.menu_bar.file;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.annotation.Nullable;
import javax.swing.JMenuItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OpenRecentMenu extends AbstractEnigmaMenu {
	protected OpenRecentMenu(Gui gui) {
		super(gui);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.file.open_recent_project"));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.removeAll();
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
			item.addActionListener(event -> this.onRecentClicked(recent));
			this.add(item);
		}
	}

	private void onRecentClicked(Config.RecentProject recent) {
		this.gui.getController().openJar(recent.getJarPath()).whenComplete((v, t) -> this.gui.getController().openMappings(recent.getMappingsPath()));
	}

	/**
	 * Find the longest common path between two absolute(!!) paths.
	 */
	@Nullable
	private static Path findCommonPath(Path a, Path b) {
		int i = 0;
		int minNameCount = Math.min(a.getNameCount(), b.getNameCount());
		for (; i < minNameCount; i++) {
			Path nameA = a.getName(i);
			Path nameB = b.getName(i);

			if (!nameA.equals(nameB)) {
				break;
			}
		}

		return i != 0 ? a.getRoot().resolve(a.subpath(0, i)) : null;
	}
}

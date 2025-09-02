package org.quiltmc.enigma.gui.element.menu_bar.file;

import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.util.ExtensionFileFilter;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SaveMappingsAsMenu extends AbstractEnigmaMenu {
	private final Map<ReadWriteService, JMenuItem> items = new HashMap<>();

	protected SaveMappingsAsMenu(Gui gui) {
		super(gui);

		this.iterateFormats(format -> {
			JMenuItem item = new JMenuItem();
			this.items.put(format, item);
			this.add(item);

			item.addActionListener(e -> this.onFormatClicked(format));
		});
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.file.mappings.save_as"));

		this.iterateFormats(format -> this.items.get(format).setText(I18n.translate(format.getId())));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.setEnabled(jarOpen);
	}

	private void onFormatClicked(ReadWriteService format) {
		JFileChooser fileChooser = this.gui.mappingsFileChooser;
		ExtensionFileFilter.setupFileChooser(this.gui, fileChooser, format);

		if (fileChooser.getCurrentDirectory() == null) {
			fileChooser.setCurrentDirectory(new File(Config.main().stats.lastSelectedDir.value()));
		}

		if (fileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			Path savePath = ExtensionFileFilter.getSavePath(fileChooser);
			this.gui.getController().saveMappings(savePath, format, false);
			Config.main().stats.lastSelectedDir.setValue(fileChooser.getCurrentDirectory().toString());
		}
	}

	private void iterateFormats(Consumer<ReadWriteService> consumer) {
		this.gui.getController().getEnigma().getReadWriteServices().forEach(format -> {
			if (format.supportsWriting()) {
				consumer.accept(format);
			}
		});
	}
}

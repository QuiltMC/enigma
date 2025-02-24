package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.Decompiler;
import org.quiltmc.enigma.gui.dialog.decompiler.DecompilerSettingsDialog;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class DecompilerMenu extends AbstractEnigmaMenu {
	private final Gui gui;

	private final JMenuItem decompilerSettingsItem = new JMenuItem();

	public DecompilerMenu(Gui gui) {
		this.gui = gui;

		ButtonGroup decompilerGroup = new ButtonGroup();

		for (Decompiler decompiler : Decompiler.values()) {
			JRadioButtonMenuItem decompilerButton = new JRadioButtonMenuItem(decompiler.name);
			decompilerGroup.add(decompilerButton);
			if (decompiler.equals(Config.decompiler().activeDecompiler.value())) {
				decompilerButton.setSelected(true);
			}

			decompilerButton.addActionListener(event -> {
				this.gui.getController().setDecompiler(decompiler.service);

				Config.decompiler().activeDecompiler.setValue(decompiler, true);
			});
			this.add(decompilerButton);
		}

		this.addSeparator();
		this.add(this.decompilerSettingsItem);

		this.decompilerSettingsItem.addActionListener(e -> DecompilerSettingsDialog.show(this.gui));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.decompiler"));
		this.decompilerSettingsItem.setText(I18n.translate("menu.decompiler.settings"));
	}
}

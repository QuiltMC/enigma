package org.quiltmc.enigma.gui.element.menu_bar.file;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.CrashDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;

public class CrashHistoryMenu extends AbstractSearchableEnigmaMenu {
	protected CrashHistoryMenu(Gui gui) {
		super(gui);
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.file.crash_history"));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.removeAll();
		ButtonGroup crashHistoryGroup = new ButtonGroup();

		for (int i = 0; i < this.gui.getCrashHistory().size(); i++) {
			Throwable t = this.gui.getCrashHistory().get(i);
			JMenuItem crashHistoryButton = new JMenuItem(i + " - " + t.toString());
			crashHistoryGroup.add(crashHistoryButton);

			crashHistoryButton.addActionListener(event -> this.onCrashClicked(t));

			this.add(crashHistoryButton);
		}

		this.setEnabled(!this.gui.getCrashHistory().isEmpty());
	}

	private void onCrashClicked(Throwable throwable) {
		CrashDialog.show(throwable, false);
	}
}

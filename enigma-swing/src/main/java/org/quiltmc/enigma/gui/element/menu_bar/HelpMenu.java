package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.AboutDialog;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenuItem;

public class HelpMenu extends AbstractEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.help";

	private final JMenuItem aboutItem = new JMenuItem();
	private final JMenuItem githubItem = new JMenuItem();
	private final SearchMenusMenu searchMenusMenu;

	public HelpMenu(Gui gui) {
		super(gui);

		this.searchMenusMenu = new SearchMenusMenu(gui);

		this.add(this.aboutItem);
		this.add(this.githubItem);
		this.add(this.searchMenusMenu);

		this.aboutItem.addActionListener(e -> AboutDialog.show(this.gui.getFrame()));
		this.githubItem.addActionListener(e -> this.onGithubClicked());
	}

	public void clearSearchMenusResults() {
		this.searchMenusMenu.clearResults();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.aboutItem.setText(I18n.translate("menu.help.about"));
		this.githubItem.setText(I18n.translate("menu.help.github"));
		this.searchMenusMenu.retranslate();
	}

	private void onGithubClicked() {
		GuiUtil.openUrl("https://github.com/QuiltMC/Enigma");
	}
}

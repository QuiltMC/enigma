package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.SearchDialog;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenuItem;

public class SearchMenu extends AbstractEnigmaMenu {
	private final Gui gui;

	private final JMenuItem searchItem = new JMenuItem(GuiUtil.DEOBFUSCATED_ICON);
	private final JMenuItem searchAllItem = new JMenuItem(GuiUtil.DEOBFUSCATED_ICON);
	private final JMenuItem searchClassItem = new JMenuItem(GuiUtil.CLASS_ICON);
	private final JMenuItem searchMethodItem = new JMenuItem(GuiUtil.METHOD_ICON);
	private final JMenuItem searchFieldItem = new JMenuItem(GuiUtil.FIELD_ICON);

	public SearchMenu(Gui gui) {
		this.gui = gui;

		this.add(this.searchItem);
		this.add(this.searchAllItem);
		this.add(this.searchClassItem);
		this.add(this.searchMethodItem);
		this.add(this.searchFieldItem);

		this.searchItem.addActionListener(e -> this.onSearchClicked(false));
		this.searchAllItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.values()));
		this.searchClassItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.CLASS));
		this.searchMethodItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.METHOD));
		this.searchFieldItem.addActionListener(e -> this.onSearchClicked(true, SearchDialog.Type.FIELD));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.search"));
		this.searchItem.setText(I18n.translate("menu.search"));
		this.searchAllItem.setText(I18n.translate("menu.search.all"));
		this.searchClassItem.setText(I18n.translate("menu.search.class"));
		this.searchMethodItem.setText(I18n.translate("menu.search.method"));
		this.searchFieldItem.setText(I18n.translate("menu.search.field"));
	}

	@Override
	public void setKeyBinds() {
		this.searchItem.setAccelerator(KeyBinds.SEARCH.toKeyStroke());
		this.searchAllItem.setAccelerator(KeyBinds.SEARCH_ALL.toKeyStroke());
		this.searchClassItem.setAccelerator(KeyBinds.SEARCH_CLASS.toKeyStroke());
		this.searchMethodItem.setAccelerator(KeyBinds.SEARCH_METHOD.toKeyStroke());
		this.searchFieldItem.setAccelerator(KeyBinds.SEARCH_FIELD.toKeyStroke());
	}

	private void onSearchClicked(boolean clear, SearchDialog.Type... types) {
		if (this.gui.getController().getProject() != null) {
			this.gui.getSearchDialog().show(clear, types);
		}
	}
}

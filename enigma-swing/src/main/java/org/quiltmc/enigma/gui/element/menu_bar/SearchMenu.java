package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.dialog.SearchDialog;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

public class SearchMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.search";

	private final SimpleItem searchItem = new SimpleItem("menu.search");
	private final SimpleItem searchAllItem = new SimpleItem("menu.search.all");
	private final SimpleItem searchClassItem = new SimpleItem("menu.search.class");
	private final SimpleItem searchMethodItem = new SimpleItem("menu.search.method");
	private final SimpleItem searchFieldItem = new SimpleItem("menu.search.field");

	public SearchMenu(Gui gui) {
		super(gui);

		this.searchItem.setIcon(GuiUtil.DEOBFUSCATED_ICON);
		this.searchAllItem.setIcon(GuiUtil.DEOBFUSCATED_ICON);
		this.searchClassItem.setIcon(GuiUtil.CLASS_ICON);
		this.searchMethodItem.setIcon(GuiUtil.METHOD_ICON);
		this.searchFieldItem.setIcon(GuiUtil.FIELD_ICON);

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
		this.setText(I18n.translate(TRANSLATION_KEY));

		this.searchItem.retranslate();
		this.searchAllItem.retranslate();
		this.searchClassItem.retranslate();
		this.searchMethodItem.retranslate();
		this.searchFieldItem.retranslate();
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

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}
}

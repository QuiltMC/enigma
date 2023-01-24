package cuchaz.enigma.gui.elements;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.utils.I18n;

public class EditorTabPopupMenu {

	private final JPopupMenu ui;
	private final JMenuItem close;
	private final JMenuItem closeAll;
	private final JMenuItem closeOthers;
	private final JMenuItem closeLeft;
	private final JMenuItem closeRight;

	private EditorPanel editor;

	public EditorTabPopupMenu(EditorTabbedPane pane) {
		this.ui = new JPopupMenu();

		this.close = new JMenuItem();
		this.close.setAccelerator(KeyBinds.EDITOR_CLOSE_TAB.toKeyStroke());
		this.close.addActionListener(a -> pane.closeEditor(this.editor));
		this.ui.add(this.close);

		this.closeAll = new JMenuItem();
		this.closeAll.addActionListener(a -> pane.closeAllEditorTabs());
		this.ui.add(this.closeAll);

		this.closeOthers = new JMenuItem();
		this.closeOthers.addActionListener(a -> pane.closeTabsExcept(this.editor));
		this.ui.add(this.closeOthers);

		this.closeLeft = new JMenuItem();
		this.closeLeft.addActionListener(a -> pane.closeTabsLeftOf(this.editor));
		this.ui.add(this.closeLeft);

		this.closeRight = new JMenuItem();
		this.closeRight.addActionListener(a -> pane.closeTabsRightOf(this.editor));
		this.ui.add(this.closeRight);

		this.retranslateUi();
	}

	public void show(Component invoker, int x, int y, EditorPanel editorPanel) {
		this.editor = editorPanel;
		this.ui.show(invoker, x, y);
	}

	public void retranslateUi() {
		this.close.setText(I18n.translate("popup_menu.editor_tab.close"));
		this.closeAll.setText(I18n.translate("popup_menu.editor_tab.close_all"));
		this.closeOthers.setText(I18n.translate("popup_menu.editor_tab.close_others"));
		this.closeLeft.setText(I18n.translate("popup_menu.editor_tab.close_left"));
		this.closeRight.setText(I18n.translate("popup_menu.editor_tab.close_right"));
	}
}

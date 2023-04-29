package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.docker.DeobfuscatedClassesDocker;
import cuchaz.enigma.utils.I18n;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

public class ClassSelectorPopupMenu {
	private final JPopupMenu ui;
	private final JMenuItem renamePackage = new JMenuItem();
	private final JMenuItem renameClass = new JMenuItem();
	private final JMenuItem toggleMapping = new JMenuItem();
	private final JMenuItem expandAll = new JMenuItem();
	private final JMenuItem collapseAll = new JMenuItem();

	public ClassSelectorPopupMenu(DeobfuscatedClassesDocker panel) {
		this.ui = new JPopupMenu();

		this.ui.add(this.renamePackage);
		this.ui.add(this.renameClass);
		this.ui.addSeparator();
		this.ui.add(this.expandAll);
		this.ui.add(this.collapseAll);

		ClassSelector selector = panel.getClassSelector();

		this.renamePackage.addActionListener(a -> {
			TreePath path;

			if (selector.getSelectedClass() != null
					&& selector.getSelectionPath() != null) {
				// Rename parent package if selected path is a class
				path = selector.getSelectionPath().getParentPath();

				// do not allow renaming if the class has no package
				if (path.getPathCount() == 1) {
					return;
				}
			} else {
				// Rename selected path if it's already a package
				path = selector.getSelectionPath();
			}

			// todo brokey
			selector.getUI().startEditingAtPath(selector, path);
		});
		this.renameClass.addActionListener(a -> {
			// todo brokey
			selector.getUI().startEditingAtPath(selector, selector.getSelectionPath());
		});

		this.expandAll.addActionListener(a -> selector.expandAll());
		this.collapseAll.addActionListener(a -> selector.collapseAll());

		this.retranslateUi();
	}

	public void show(ClassSelector selector, int x, int y) {
		// Only enable rename class if selected path is a class
		this.renameClass.setEnabled(selector.getSelectedClass() != null);
		// todo mark as deob / mark as ob

		this.ui.show(selector, x, y);
	}

	public void retranslateUi() {
		this.renamePackage.setText(I18n.translate("popup_menu.deobf_panel.rename_package"));
		this.renameClass.setText(I18n.translate("popup_menu.deobf_panel.rename_class"));
		this.expandAll.setText(I18n.translate("popup_menu.deobf_panel.expand_all"));
		this.collapseAll.setText(I18n.translate("popup_menu.deobf_panel.collapse_all"));
	}
}

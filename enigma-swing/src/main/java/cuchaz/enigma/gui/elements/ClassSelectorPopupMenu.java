package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.DeobfuscatedClassesDocker;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayDeque;
import java.util.Deque;

public class ClassSelectorPopupMenu {
	private final Gui gui;
	private final JPopupMenu ui;
	private final JMenuItem renamePackage = new JMenuItem();
	private final JMenuItem renameClass = new JMenuItem();
	private final JMenuItem toggleMapping = new JMenuItem();
	private final JMenuItem expandAll = new JMenuItem();
	private final JMenuItem collapseAll = new JMenuItem();

	public ClassSelectorPopupMenu(Gui gui, DeobfuscatedClassesDocker panel) {
		this.gui = gui;
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

			if (path == null) {
				return;
			}

			StringBuilder pathString = new StringBuilder();
			for (int i = 0; i < path.getPathCount(); i++) {
				String component = path.getPathComponent(i).toString();

				if (!component.isBlank()) {
					pathString.append(component);
					if (i < path.getPathCount() - 1) {
						pathString.append("/");
					}
				}
			}

			String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("gaming"), pathString.toString());
			String[] oldPackageNames = pathString.toString().split("/");
			String[] newPackageNames = input.split("/");

			for (int i = 0; i < selector.getPackageManager().getRoot().getChildCount(); i++) {
				TreeNode node = selector.getPackageManager().getRoot().getChildAt(i);
				Deque<Runnable> renameStack = new ArrayDeque<>();

				this.handleNode(0, 0, false, oldPackageNames, newPackageNames, renameStack, node);
				renameStack.forEach(Runnable::run);
			}
		});

		this.renameClass.addActionListener(a -> {
			String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("gaming"), selector.getSelectedClass().getFullName());
			this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(selector.getSelectedClass()).withDeobfName(input));
		});

		this.expandAll.addActionListener(a -> selector.expandAll());
		this.collapseAll.addActionListener(a -> selector.collapseAll());

		this.retranslateUi();
	}

	private void handleNode(int divergenceIndex, int realIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Deque<Runnable> renameStack, TreeNode node) {
		if (node instanceof ClassSelectorClassNode classNode && rename) {
			String oldName = classNode.getClassEntry().getFullName();
			int finalPackageIndex = divergenceIndex - 1;
			renameStack.push(() -> {
				String[] split = oldName.split("/");
				StringBuilder newPackages = new StringBuilder();

				for (int i = finalPackageIndex; i < newPackageNames.length; i++) {
					if (i >= 0) {
						if (i < oldPackageNames.length && i < split.length && oldPackageNames[i].equals(split[i])) {
							split[i] = newPackageNames[i];
						} else {
							newPackages.append("/").append(newPackageNames[i]);
						}
					}
				}

				// append new packages to last package
				if (!newPackages.toString().isBlank()) {
					split[newPackageNames.length - 2] = split[newPackageNames.length - 2] + newPackages;
				}

				String newName = String.join("/", split);
				this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(classNode.getClassEntry()).withDeobfName(newName));
			});
		} else if (node instanceof ClassSelectorPackageNode packageNode) {
			String packageName = packageNode.getPackageName().substring(packageNode.getPackageName().lastIndexOf("/") + 1);

			if (packageName.equals(oldPackageNames[divergenceIndex])) {
				if (!rename) {
					if (packageName.equals(newPackageNames[divergenceIndex])) {
						this.handlePackage(divergenceIndex, realIndex, false, oldPackageNames, newPackageNames, renameStack, packageNode);
					} else {
						// use parent package to begin rename
						realIndex--;
						this.handlePackage(divergenceIndex, realIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode.getParent());
					}
				} else {
					this.handlePackage(divergenceIndex, realIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode);
				}
			} else if (rename && realIndex >= oldPackageNames.length) {
				this.handlePackage(divergenceIndex, realIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode);
			}
		}
	}

	private void handlePackage(int divergenceIndex, int realIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Deque<Runnable> renameStack, TreeNode packageNode) {
		realIndex++;
		if (!rename) {
			divergenceIndex++;
		}

		for (int j = 0; j < packageNode.getChildCount(); j++) {
			this.handleNode(divergenceIndex, realIndex, rename, oldPackageNames, newPackageNames, renameStack, packageNode.getChildAt(j));
		}
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

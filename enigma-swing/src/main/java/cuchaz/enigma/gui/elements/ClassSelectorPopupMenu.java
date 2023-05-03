package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.ProgressDialog;
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
import java.util.ArrayList;
import java.util.List;

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

			if (input == null || !input.matches("[a-z_/]+") || input.isBlank() || input.startsWith("/") || input.endsWith("/")) {
				//this.gui.getNotificationManager().notify(Message.INVALID_PACKAGE);
				return;
			}

			String[] oldPackageNames = pathString.toString().split("/");
			String[] newPackageNames = input.split("/");

			List<Runnable> renameStack = new ArrayList<>();

			ProgressDialog.runOffThread(this.gui.getFrame(), listener -> {
				listener.init(1, "discovering classes to rename");

				for (int i = 0; i < selector.getPackageManager().getRoot().getChildCount(); i++) {
					TreeNode node = selector.getPackageManager().getRoot().getChildAt(i);
					this.handleNode(0, false, oldPackageNames, newPackageNames, renameStack, node);
				}

				listener.init(renameStack.size(), "renaming classes");

				for (int j = 0; j < renameStack.size(); j++) {
					listener.step(j, "still renaming classes");
					renameStack.get(j).run();
				}
			});
		});

		this.renameClass.addActionListener(a -> {
			String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("gaming"), selector.getSelectedClass().getFullName());
			this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(selector.getSelectedClass(true)).withDeobfName(input));
		});

		this.expandAll.addActionListener(a -> selector.expandAll());
		this.collapseAll.addActionListener(a -> selector.collapseAll());

		this.retranslateUi();
	}

	private void handleNode(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, List<Runnable> renameStack, TreeNode node) {
		if (node instanceof ClassSelectorClassNode classNode && rename) {
			String oldName = classNode.getClassEntry().getFullName();
			int finalPackageIndex = divergenceIndex - 1;

			renameStack.add(() -> {
				String[] split = oldName.split("/");
				StringBuilder newPackages = new StringBuilder();

				if (oldPackageNames.length <= newPackageNames.length) {
					for (int i = finalPackageIndex; i < newPackageNames.length; i++) {
						if (i >= 0) {
							if (i < oldPackageNames.length && i < split.length && oldPackageNames[i].equals(split[i])) {
								split[i] = newPackageNames[i];
							} else {
								newPackages.append("/").append(newPackageNames[i]);
							}
						}
					}
				} else {
					for (int i = 0; i < oldPackageNames.length; i++) {
						if (i > newPackageNames.length - 1 || !oldPackageNames[i].equals(newPackageNames[i])) {
							StringBuilder string = new StringBuilder();
							for (int j = 0; j <= i - 1; j++) {
								if (!string.isEmpty()) {
									string.append("/");
								}

								string.append(oldPackageNames[j]);
							}

							for (int j = i; j < newPackageNames.length; j++) {
								if (!string.isEmpty()) {
									string.append("/");
								}

								string.append(newPackageNames[j]);
							}

							if (!string.isEmpty()) {
								string.append("/");
							}
							string.append(classNode.getClassEntry().getSimpleName());
							split = string.toString().split("/");
							break;
						}
					}
				}

				// append new packages to last package
				if (!newPackages.toString().isBlank()) {
					split[finalPackageIndex] = split[finalPackageIndex] + newPackages;
				}

				String newName = String.join("/", split);
				this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(classNode.getObfEntry()).withDeobfName(newName));
			});
		} else if (node instanceof ClassSelectorPackageNode packageNode) {
			// todo does not handle the possibility of going backwards instead of forwards
			String packageName = packageNode.getPackageName().substring(packageNode.getPackageName().lastIndexOf("/") + 1);
			int index = packageNode.getPackageName().split("/").length;

			if (oldPackageNames.length > newPackageNames.length) {
				if (divergenceIndex == 0 || packageName.equals(oldPackageNames[divergenceIndex])) {
					if (rename) {
						this.handlePackage(divergenceIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode);
					} else if (divergenceIndex < newPackageNames.length && newPackageNames[divergenceIndex].equals(oldPackageNames[divergenceIndex])) {
						this.handlePackage(divergenceIndex, false, oldPackageNames, newPackageNames, renameStack, packageNode);
					} else {
						this.handlePackage(index, true, oldPackageNames, newPackageNames, renameStack, packageNode);
					}

					return;
				} else {
					this.handlePackage(index, false, oldPackageNames, newPackageNames, renameStack, packageNode);
				}
			}

			if ((divergenceIndex >= oldPackageNames.length && index < newPackageNames.length) || packageName.equals(oldPackageNames[divergenceIndex])) {
				if (!rename) {
					if (packageName.equals(newPackageNames[divergenceIndex])) {
						if (newPackageNames.length > oldPackageNames.length && divergenceIndex == oldPackageNames.length - 1) {
							this.handlePackage(divergenceIndex + 1, true, oldPackageNames, newPackageNames, renameStack, packageNode);
						}

						this.handlePackage(divergenceIndex, false, oldPackageNames, newPackageNames, renameStack, packageNode);
					} else {
						// use parent package to begin rename
						this.handlePackage(divergenceIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode.getParent());
					}
				} else {
					this.handlePackage(divergenceIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode);
				}
			} else if (rename && index >= oldPackageNames.length) {
				this.handlePackage(divergenceIndex, true, oldPackageNames, newPackageNames, renameStack, packageNode);
			}
		}
	}

	private void handlePackage(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, List<Runnable> renameStack, TreeNode node) {
		if (!rename) {
			divergenceIndex++;
		}

		for (int j = 0; j < node.getChildCount(); j++) {
			this.handleNode(divergenceIndex, rename, oldPackageNames, newPackageNames, renameStack, node.getChildAt(j));
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

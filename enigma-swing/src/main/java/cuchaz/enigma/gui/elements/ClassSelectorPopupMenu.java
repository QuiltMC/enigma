package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.docker.ClassesDocker;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClassSelectorPopupMenu {
	private final Gui gui;
	private final ClassSelector selector;
	private final JPopupMenu ui;

	private final JMenuItem renamePackage = new JMenuItem();
	private final JMenuItem renameClass = new JMenuItem();
	private final JMenuItem toggleMapping = new JMenuItem();
	private final JMenuItem expandAll = new JMenuItem();
	private final JMenuItem collapseAll = new JMenuItem();

	public ClassSelectorPopupMenu(Gui gui, ClassesDocker docker) {
		this.gui = gui;
		this.selector = docker.getClassSelector();
		this.ui = new JPopupMenu();

		this.ui.add(this.renamePackage);
		this.ui.add(this.renameClass);
		this.ui.add(this.toggleMapping);
		this.ui.addSeparator();
		this.ui.add(this.expandAll);
		this.ui.add(this.collapseAll);

		this.renamePackage.addActionListener(a -> this.onRenamePackage());

		this.renameClass.addActionListener(a -> {
			String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("popup_menu.class_selector.rename_class"), this.selector.getSelectedClassDeobf().getFullName());
			this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(this.selector.getSelectedClassObf()).withDeobfName(input));
		});

		this.toggleMapping.addActionListener(a -> {
			ClassEntry classEntry = this.selector.getSelectedClassObf();
			if (classEntry == null) {
				return;
			}

			this.gui.toggleMappingFromEntry(classEntry);
		});

		this.expandAll.addActionListener(a -> this.selector.expandAll());
		this.collapseAll.addActionListener(a -> this.selector.collapseAll());

		this.retranslateUi();
	}

	private void onRenamePackage() {
		TreePath path;

		if (this.selector.getSelectedClassDeobf() != null
				&& this.selector.getSelectionPath() != null) {
			// Rename parent package if selected path is a class
			path = this.selector.getSelectionPath().getParentPath();

			// do not allow renaming if the class has no package
			if (path.getPathCount() == 1) {
				return;
			}
		} else {
			// Rename selected path if it's already a package
			path = this.selector.getSelectionPath();
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

		String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("popup_menu.class_selector.package_rename.title"), pathString.toString());
		this.renamePackage(pathString.toString(), input);
	}

	public CompletableFuture<Void> renamePackage(String path, String input) {
		if (input == null || !input.matches("[a-z_/]+") || input.isBlank() || input.startsWith("/") || input.endsWith("/")) {
			this.gui.getNotificationManager().notify(Message.INVALID_PACKAGE_NAME);
			return CompletableFuture.supplyAsync(() -> null);
		}

		String[] oldPackageNames = path.split("/");
		String[] newPackageNames = input.split("/");

		Map<String, Runnable> renameStack = new HashMap<>();

		return ProgressDialog.runOffThread(this.gui, listener -> {
			listener.init(1, I18n.translate("popup_menu.class_selector.package_rename.discovering"));
			TreeNode root = this.selector.getPackageManager().getRoot();

			for (int i = 0; i < root.getChildCount(); i++) {
				TreeNode node = root.getChildAt(i);
				this.handleNode(0, false, oldPackageNames, newPackageNames, renameStack, node);
			}

			listener.init(renameStack.size(), I18n.translate("popup_menu.class_selector.package_rename.renaming_classes"));

			Map<ClassesDocker, List<ClassSelector.StateEntry>> expansionStates = new HashMap<>();
			for (Docker docker : this.gui.getDockerManager().getDockers()) {
				if (docker instanceof ClassesDocker classesDocker) {
					expansionStates.put(classesDocker, classesDocker.getClassSelector().getExpansionState());
				}
			}

			int i = 0;
			for (var entry : renameStack.entrySet()) {
				listener.step(i, I18n.translateFormatted("popup_menu.class_selector.package_rename.renaming_class", entry.getKey()));
				entry.getValue().run();
				i++;
			}

			for (var entry : expansionStates.entrySet()) {
				ClassSelector classSelector = entry.getKey().getClassSelector();
				classSelector.reload();
				classSelector.restoreExpansionState(entry.getValue());
			}
		});
	}

	private void handleNode(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Map<String, Runnable> renameStack, TreeNode node) {
		if (node instanceof ClassSelectorClassNode classNode && rename) {
			String oldName = classNode.getDeobfEntry().getFullName();
			int finalPackageIndex = divergenceIndex - 1;

			renameStack.put(oldName, () -> {
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

							// append preceding old package names
							for (int j = 0; j <= i - 1; j++) {
								appendSlash(string);
								string.append(oldPackageNames[j]);
							}

							// append new package names
							for (int j = i; j < newPackageNames.length; j++) {
								appendSlash(string);
								string.append(newPackageNames[j]);
							}

							// append the remaining old package names
							for (int j = i - 1 + oldPackageNames.length; j < split.length - 1; j++) {
								appendSlash(string);
								string.append(split[j]);
							}

							appendSlash(string);
							string.append(classNode.getDeobfEntry().getSimpleName());
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
				this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(classNode.getObfEntry()).withDeobfName(newName), false);
			});
		} else if (node instanceof ClassSelectorPackageNode packageNode) {
			String packageName = packageNode.getPackageName().substring(packageNode.getPackageName().lastIndexOf("/") + 1);
			int index = packageNode.getPackageName().split("/").length - 1;

			if (rename) {
				this.handlePackage(divergenceIndex, true, oldPackageNames, newPackageNames, renameStack, node);
				return;
			}

			// handle backwards renaming
			if (oldPackageNames.length > newPackageNames.length) {
				// if we are past the final index in the new packages, begin rename from previous
				if (newPackageNames.length <= index) {
					this.handlePackage(index - 1, true, oldPackageNames, newPackageNames, renameStack, node.getParent());
					return;
				}
			} else {
				// handle appending new packages
				if (newPackageNames.length > oldPackageNames.length && index == oldPackageNames.length - 1) {
					this.handlePackage(index + 1, true, oldPackageNames, newPackageNames, renameStack, packageNode);
					return;
				}
			}

			if (packageName.equals(newPackageNames[index])) {
				this.handlePackage(index, false, oldPackageNames, newPackageNames, renameStack, packageNode);
			} else if (packageName.equals(oldPackageNames[index])) {
				this.handlePackage(index, true, oldPackageNames, newPackageNames, renameStack, packageNode);
			}
		}
	}

	private static void appendSlash(StringBuilder string) {
		if (!string.isEmpty()) {
			string.append("/");
		}
	}

	private void handlePackage(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Map<String, Runnable> renameStack, TreeNode node) {
		if (!rename) {
			divergenceIndex++;
		}

		for (int j = 0; j < node.getChildCount(); j++) {
			this.handleNode(divergenceIndex, rename, oldPackageNames, newPackageNames, renameStack, node.getChildAt(j));
		}
	}

	public void show(ClassSelector selector, int x, int y) {
		ClassEntry selected = selector.getSelectedClassObf();

		// only enable rename class if selected path is a class
		this.renameClass.setEnabled(selected != null);

		// only enable rename package if selected path is *not* a class with no package
		this.renamePackage.setEnabled(selected == null || selector.getSelectedClassDeobf().getPackageName() != null);

		// update toggle mapping text to match
		this.toggleMapping.setEnabled(selected != null);
		if (selected != null) {
			if (this.gui.getController().getProject().getMapper().extendedDeobfuscate(selected).isDeobfuscated()) {
				this.toggleMapping.setText(I18n.translate("popup_menu.reset_obfuscated"));
			} else {
				this.toggleMapping.setText(I18n.translate("popup_menu.mark_deobfuscated"));
			}
		}

		this.ui.show(selector, x, y);
	}

	public void retranslateUi() {
		this.renamePackage.setText(I18n.translate("popup_menu.class_selector.rename_package"));
		this.renameClass.setText(I18n.translate("popup_menu.class_selector.rename_class"));
		this.expandAll.setText(I18n.translate("popup_menu.class_selector.expand_all"));
		this.collapseAll.setText(I18n.translate("popup_menu.class_selector.collapse_all"));
		this.toggleMapping.setText(I18n.translate("popup_menu.mark_deobfuscated"));
	}
}

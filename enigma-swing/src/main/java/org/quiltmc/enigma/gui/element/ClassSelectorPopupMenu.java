package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.util.PackageRenamer;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.ValidationContext;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

public class ClassSelectorPopupMenu {
	private final Gui gui;
	private final ClassSelector selector;
	private final JPopupMenu ui;

	private final JMenuItem renamePackage = new JMenuItem();
	private final JMenuItem movePackage = new JMenuItem();
	private final JMenuItem renameClass = new JMenuItem();
	private final JMenuItem regenerateStats = new JMenuItem();
	private final JMenuItem toggleMapping = new JMenuItem();
	private final JMenuItem expandAll = new JMenuItem();
	private final JMenuItem collapseAll = new JMenuItem();

	public ClassSelectorPopupMenu(Gui gui, ClassesDocker docker) {
		this.gui = gui;
		this.selector = docker.getClassSelector();
		this.ui = new JPopupMenu();

		this.ui.add(this.renamePackage);
		this.ui.add(this.movePackage);
		this.ui.add(this.renameClass);
		this.ui.add(this.regenerateStats);
		this.ui.add(this.toggleMapping);
		this.ui.addSeparator();
		this.ui.add(this.expandAll);
		this.ui.add(this.collapseAll);

		this.renamePackage.addActionListener(a -> this.onRenamePackage(PackageRenamer.Mode.REFACTOR));
		this.movePackage.addActionListener(a -> this.onRenamePackage(PackageRenamer.Mode.MOVE));

		this.renameClass.addActionListener(a -> {
			String input = JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("popup_menu.class_selector.rename_class"), this.selector.getSelectedClassDeobf().getFullName());
			if (input == null) {
				return;
			}

			this.gui.getController().applyChange(new ValidationContext(this.gui.getNotificationManager()), EntryChange.modify(this.selector.getSelectedClassObf()).withDeobfName(input));
		});

		this.toggleMapping.addActionListener(a -> {
			ClassEntry classEntry = this.selector.getSelectedClassObf();
			if (classEntry == null) {
				return;
			}

			this.gui.toggleMappingFromEntry(classEntry);
		});

		this.regenerateStats.addActionListener(a -> this.gui.reloadStats(this.selector.getSelectedClassObf(), false));

		this.expandAll.addActionListener(a -> this.selector.expandAll());
		this.collapseAll.addActionListener(a -> this.selector.collapseAll());

		this.retranslateUi();
	}

	private void onRenamePackage(PackageRenamer.Mode mode) {
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

		String title = switch (mode) {
			case MOVE -> I18n.translateFormatted("popup_menu.class_selector.package_rename.move_title", pathString.toString());
			case REFACTOR -> I18n.translateFormatted("popup_menu.class_selector.package_rename.rename_title", pathString.toString());
		};

		String input = JOptionPane.showInputDialog(this.gui.getFrame(), title, pathString.toString());
		if (input != null) {
			this.createPackageRenamer(mode).renamePackage(pathString.toString(), input);
		}
	}

	public PackageRenamer createPackageRenamer(PackageRenamer.Mode mode) {
		return new PackageRenamer(this.gui, this.selector, mode);
	}

	public void show(ClassSelector selector, int x, int y) {
		ClassEntry selected = selector.getSelectedClassObf();

		// only enable rename class if selected path is a class
		this.renameClass.setEnabled(selected != null);

		// only enable rename package if selected path is *not* a class with no package
		this.renamePackage.setEnabled(selected == null || selector.getSelectedClassDeobf().getPackageName() != null);

		// only enable regenerate stats if selected path is a class
		this.regenerateStats.setEnabled(selected != null);

		// update toggle mapping text to match
		this.toggleMapping.setEnabled(selected != null);
		if (selected != null) {
			if (this.gui.getController().getProject().getRemapper().extendedDeobfuscate(selected).getType() == TokenType.DEOBFUSCATED) {
				this.toggleMapping.setText(I18n.translate("popup_menu.reset_obfuscated"));
			} else {
				this.toggleMapping.setText(I18n.translate("popup_menu.mark_deobfuscated"));
			}
		}

		this.ui.show(selector, x, y);
	}

	public void retranslateUi() {
		this.renamePackage.setText(I18n.translate("popup_menu.class_selector.rename_package"));
		this.movePackage.setText(I18n.translate("popup_menu.class_selector.move_package"));
		this.renameClass.setText(I18n.translate("popup_menu.class_selector.rename_class"));
		this.expandAll.setText(I18n.translate("popup_menu.class_selector.expand_all"));
		this.collapseAll.setText(I18n.translate("popup_menu.class_selector.collapse_all"));
		this.toggleMapping.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		this.regenerateStats.setText(I18n.translate("popup_menu.class_selector.regenerate_stats"));
	}
}

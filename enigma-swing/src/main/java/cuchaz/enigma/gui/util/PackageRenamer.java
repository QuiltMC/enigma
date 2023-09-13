package cuchaz.enigma.gui.util;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.docker.ClassesDocker;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.ClassSelectorPackageNode;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.tree.TreeNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PackageRenamer {
	private final Gui gui;
	private final ClassSelector selector;
	private final Mode mode;

	/**
	 * Creates a new package renamer that will use the given mode.
	 */
	public PackageRenamer(Gui gui, ClassSelector selector, Mode mode) {
		this.gui = gui;
		this.selector = selector;
		this.mode = mode;
	}

	/**
	 * Represents a different way that packages will be refactored.
	 */
	public enum Mode {
		/**
		 * Renames packages according to your input.
		 * Takes the point where the package diverge, and refactors all classes in packages past that point.
		 * For example, when renaming the package {@code a/b/c} to {@code a/c}, Enigma will consider that you want to rename {@code b/c} to simply {@code c},
		 * meaning that classes like {@code a/b/C} would end up merged into the package {@code a}: {@code a/C}.
		 * Examples:
		 * <ul>
		 *     <li>
		 *         Input: {@code a/b/c} -> {@code a/b/d}
		 *         <br>Result:
		 *         <ul>
		 *             <li>{@code A} -> no change</li>
		 *             <li>{@code a/B} -> no change</li>
		 *	           <li>{@code a/b/C} -> no change</li>
		 *             <li>{@code a/b/c/D} -> {@code a/b/d/D}</li>
		 *             <li>{@code a/b/c/d/E} -> {@code a/b/d/d/E}</li>
		 *         </ul>
		 *     </li>
		 *     <li>
		 *         Input: {@code a/b/c} -> {@code a/c}
		 *         <br>Result:
		 *         <ul>
		 *             <li>{@code A} -> no change</li>
		 *             <li>{@code a/B} -> no change</li>
		 *             <li>{@code a/b/C} -> {@code a/C}</li>
		 *             <li>{@code a/b/c/D} -> {@code a/c/D}</li>
		 *             <li>{@code a/b/c/d/E} -> {@code a/c/d/E}</li>
		 *         </ul>
		 *     </li>
		 *     <li>
		 *         Input: {@code a/b/c} -> {@code a}
		 *         <br>Result:
		 *         <ul>
		 *             <li>{@code A} -> no change</li>
		 *             <li>{@code a/B} -> no change</li>
		 * 		       <li>{@code a/b/C} -> no change</li>
		 *             <li>{@code a/b/c/D} -> {@code a/D}</li>
		 *             <li>{@code a/b/c/d/E} -> {@code a/d/E}</li>
		 *         </ul>
		 *     </li>
		 * </ul>
		 */
		REFACTOR,
		/**
		 * Moves the deepest package in the hierarchy according to your input.
		 *     Examples:
		 *     <ul>
		 *         <li>
		 *             Input: {@code a/b/c} -> {@code a/b/d}
		 *             <br>Result:
		 *             <ul>
		 *                 <li>{@code A} -> no change</li>
		 *                 <li>{@code a/B} -> no change</li>
		 * 		           <li>{@code a/b/C} -> no change</li>
		 *                 <li>{@code a/b/c/D} -> {@code a/b/d/D}</li>
		 *                 <li>{@code a/b/c/d/E} -> {@code a/b/d/d/E}</li>
		 *             </ul>
		 *         </li>
		 *         <li>
		 *             Input: {@code a/b/c} -> {@code a/c}
		 *             <br>Result:
		 *             <ul>
		 *                 <li>{@code A} -> no change</li>
		 *                 <li>{@code a/B} -> no change</li>
		 * 		           <li>{@code a/b/C} -> no change</li>
		 *                 <li>{@code a/b/c/D} -> {@code a/c/D}</li>
		 *                 <li>{@code a/b/c/d/E} -> {@code a/c/d/E}</li>
		 *             </ul>
		 *         </li>
		 *         <li>
		 *             Input: {@code a/b/c} -> {@code a}
		 *             <br>Result:
		 *             <ul>
		 *                 <li>{@code A} -> no change</li>
		 *                 <li>{@code a/B} -> no change</li>
		 * 		           <li>{@code a/b/C} -> no change</li>
		 *                 <li>{@code a/b/c/D} -> {@code a/c/D}</li>
		 *                 <li>{@code a/b/c/d/E} -> {@code a/c/d/E}</li>
		 *             </ul>
		 *         </li>
		 *     </ul>
		 */
		MOVE
	}

	/**
	 * Renames all classes inside the provided package recursively to match the new name.
	 * Result will differ according to the given {@link Mode}, refer to the mode's documentation for more info.
	 * @param path the original package
	 * @param input the new name for the package
	 * @return a future that will complete when all classes have been renamed
	 */
	public CompletableFuture<Void> renamePackage(String path, String input) {
		// validate input
		if (input == null || !input.matches("[a-z0-9_/]+") || input.isBlank() || input.startsWith("/") || input.endsWith("/")) {
			this.gui.getNotificationManager().notify(Message.INVALID_PACKAGE_NAME);
			return CompletableFuture.supplyAsync(() -> null);
		}

		// skip if identical
		if (path.equals(input)) {
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

			// skips all classes that do not match the exact package being renamed
			if (this.mode == Mode.MOVE) {
				if (!oldName.equals(String.join("/", oldPackageNames) + "/" + classNode.getDeobfEntry().getSimpleName())) {
					System.out.println("ignoring: " + oldName);
					return;
				}
			}

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
}

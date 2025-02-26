package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.dialog.ProgressDialog;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.node.ClassSelectorClassNode;
import org.quiltmc.enigma.gui.node.ClassSelectorPackageNode;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ValidationContext;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
		 *             <li>{@code a/b/C} -> no change</li>
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
		if (input == null || !input.matches("[a-zA-Z0-9_/]+") || input.isBlank() || input.startsWith("/") || input.endsWith("/")) {
			this.gui.getNotificationManager().notify(Message.INVALID_PACKAGE_NAME);
			return CompletableFuture.supplyAsync(() -> null);
		}

		// skip if identical
		if (path.equals(input)) {
			return CompletableFuture.supplyAsync(() -> null);
		}

		String[] oldPackageNames = path.split("/");
		String[] newPackageNames = input.split("/");

		Map<String, ClassRename> renameStack = new HashMap<>();

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

			boolean confirmed = false;
			int i = 0;
			for (var entry : renameStack.entrySet()) {
				if (!confirmed && !this.gui.isTestEnvironment()) {
					int continueOperation = JOptionPane.showConfirmDialog(this.gui.getFrame(), buildConfirmationPanel(renameStack));
					if (continueOperation != JOptionPane.YES_OPTION) {
						return;
					} else {
						confirmed = true;
					}
				}

				listener.step(i, I18n.translateFormatted("popup_menu.class_selector.package_rename.renaming_class", entry.getKey()));
				entry.getValue().executeRename();
				i++;
			}

			for (var entry : expansionStates.entrySet()) {
				ClassSelector classSelector = entry.getKey().getClassSelector();
				classSelector.reload();
				classSelector.restoreExpansionState(entry.getValue());
			}
		});
	}

	private static JPanel buildConfirmationPanel(Map<String, ClassRename> renameStack) {
		JPanel panel = new JPanel(new BorderLayout());
		int truncationThreshold = 50;

		var sampleRenameLines = collectSampleRenames(truncationThreshold, renameStack);
		JTextArea text = new JTextArea(multilineify(false, sampleRenameLines));
		text.setEditable(false);
		JScrollPane sampleRenames = new JScrollPane(text);
		sampleRenames.setPreferredSize(new Dimension(ScaleUtil.scale(400), ScaleUtil.scale(100)));

		String changesString = I18n.translate("popup_menu.class_selector.package_rename.changes_to_apply") + (sampleRenameLines.length == truncationThreshold ? " (" + I18n.translate("popup_menu.class_selector.package_rename.truncated") + ")" : "");
		panel.add(BorderLayout.NORTH, new JLabel(multilineify(true, I18n.translate("popup_menu.class_selector.package_rename.confirm_rename"), changesString)));
		panel.add(sampleRenames, BorderLayout.CENTER);

		return panel;
	}

	private static String multilineify(boolean html, String... lines) {
		StringBuilder builder = new StringBuilder(html ? "<html>" : "");

		for (int i = 0; i < lines.length; i++) {
			builder.append(lines[i]).append(i == lines.length - 1 ? "" : (html ? "<br>" : "\n"));
		}

		return builder.append(html ? "</html>" : "").toString();
	}

	private static String[] collectSampleRenames(int truncationThreshold, Map<String, ClassRename> renameStack) {
		int max = Math.min(renameStack.size(), truncationThreshold);
		int index = 0;

		String[] builder = new String[max];
		for (Map.Entry<String, ClassRename> entry : renameStack.entrySet()) {
			builder[index] = entry.getKey() + " -> " + entry.getValue().getNewName();
			index++;

			if (index >= max) {
				return builder;
			}
		}

		throw new RuntimeException("failed to collect sample renames!");
	}

	private void handleNode(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Map<String, ClassRename> renameStack, TreeNode node) {
		if (node instanceof ClassSelectorClassNode classNode && rename) {
			String oldName = classNode.getDeobfEntry().getFullName();
			int finalPackageIndex = divergenceIndex == 0 ? 0 : divergenceIndex - 1;

			// skips all classes that do not match the exact package being renamed
			if (this.mode == Mode.MOVE) {
				if (!oldName.equals(String.join("/", oldPackageNames) + "/" + classNode.getDeobfEntry().getSimpleName())) {
					return;
				}
			}

			renameStack.put(oldName, new ClassRename() {
				private String cachedNewName = null;

				@Override
				public void executeRename() {
					String newName = this.cachedNewName == null ? this.getNewName() : this.cachedNewName;

					// ignore warnings, we don't want to bother the user with every individual package created
					PackageRenamer.this.gui.getController().applyChange(new ValidationContext(PackageRenamer.this.gui.getNotificationManager(), false), EntryChange.modify(classNode.getObfEntry()).withDeobfName(newName), false);
				}

				@Override
				public String getNewName() {
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
					this.cachedNewName = newName;
					return newName;
				}
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

			if (newPackageNames.length - 1 >= index && packageName.equals(newPackageNames[index])) {
				this.handlePackage(index, false, oldPackageNames, newPackageNames, renameStack, packageNode);
			} else if (oldPackageNames.length - 1 >= index && packageName.equals(oldPackageNames[index])) {
				this.handlePackage(index, true, oldPackageNames, newPackageNames, renameStack, packageNode);
			}
		}
	}

	private static void appendSlash(StringBuilder string) {
		if (!string.isEmpty()) {
			string.append("/");
		}
	}

	private void handlePackage(int divergenceIndex, boolean rename, String[] oldPackageNames, String[] newPackageNames, Map<String, ClassRename> renameStack, TreeNode node) {
		if (!rename) {
			divergenceIndex++;
		}

		for (int j = 0; j < node.getChildCount(); j++) {
			this.handleNode(divergenceIndex, rename, oldPackageNames, newPackageNames, renameStack, node.getChildAt(j));
		}
	}

	private interface ClassRename {
		void executeRename();

		String getNewName();
	}
}

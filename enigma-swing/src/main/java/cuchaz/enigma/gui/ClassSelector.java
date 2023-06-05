package cuchaz.enigma.gui;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.node.SortedMutableTreeNode;
import cuchaz.enigma.stats.StatType;
import cuchaz.enigma.gui.util.StatsManager;
import cuchaz.enigma.stats.StatsResult;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;

import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ClassSelector extends JTree {
	public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = Comparator.comparing(ClassEntry::getFullName);

	private final Comparator<ClassEntry> comparator;
	private final GuiController controller;
	private final StatsManager statsManager;

	private NestedPackages packageManager;
	private ClassSelectionListener selectionListener;

	public ClassSelector(Gui gui, Comparator<ClassEntry> comparator) {
		this.comparator = comparator;
		this.controller = gui.getController();
		this.statsManager = gui.getStatsManager();

		// configure the tree control
		this.setEditable(false);
		this.setRootVisible(false);
		this.setShowsRootHandles(false);
		this.setModel(null);

		// hook events
		this.addMouseListener(GuiUtil.onMouseClick(event -> {
			if (this.selectionListener != null && event.getClickCount() == 2) {
				// get the selected node
				TreePath path = this.getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
					this.selectionListener.onSelectClass(node.getObfEntry());
				}
			}
		}));

		this.addKeyListener(GuiUtil.onKeyPress(e -> {
			TreePath[] paths = this.getSelectionPaths();

			if (paths != null) {
				if (KeyBinds.EDITOR_TOGGLE_MAPPING.matches(e)) {
					for (TreePath path : paths) {
						if (path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
							gui.toggleMappingFromEntry(node.getObfEntry());
						}
					}
				}

				if (this.selectionListener != null && KeyBinds.SELECT.matches(e)) {
					for (TreePath path : paths) {
						if (path.getLastPathComponent() instanceof ClassSelectorClassNode node) {
							this.selectionListener.onSelectClass(node.getObfEntry());
						}
					}
				}
			}
		}));

		this.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

				if (gui.getController().getProject() != null && leaf && value instanceof ClassSelectorClassNode node) {
					class TooltipPanel extends JPanel {
						@Override
						public String getToolTipText(MouseEvent event) {
							StringBuilder text = new StringBuilder(I18n.translateFormatted("class_selector.tooltip.stats_for", node.getDeobfEntry().getSimpleName()));
							text.append("\n");
							StatsResult stats = ClassSelector.this.statsManager.getStats(node);

							if (stats == null) {
								text.append(I18n.translate("class_selector.tooltip.stats_not_generated"));
							} else {
								if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
									for (int i = 0; i < StatType.values().length; i++) {
										StatType type = StatType.values()[i];
										text.append(type.getName()).append(": ").append(stats.toString(type)).append(i == StatType.values().length - 1 ? "" : "\n");
									}
								} else {
									text.append(stats);
								}
							}

							return text.toString();
						}
					}

					JPanel panel = new TooltipPanel();
					panel.setOpaque(false);
					panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
					JLabel nodeLabel = new JLabel(GuiUtil.getClassIcon(gui, node.getObfEntry()));
					panel.add(nodeLabel);

					StatsResult stats = ClassSelector.this.statsManager.getStats(node);
					if (stats == null) {
						// calculate stats on a separate thread for performance reasons
						this.setIcon(GuiUtil.PENDING_STATUS_ICON);
						node.reloadStats(gui, ClassSelector.this, false);
					} else {
						this.setIcon(GuiUtil.getDeobfuscationIcon(stats));
					}

					panel.add(this);

					return panel;
				}

				return this;
			}
		});

		ToolTipManager.sharedInstance().registerComponent(this);

		// init defaults
		this.selectionListener = null;
	}

	/**
	 * Sets this tree's selection listener. The listener is fired when the user clicks on a class.
	 *
	 * @param listener the new listener
	 */
	public void setSelectionListener(ClassSelectionListener listener) {
		this.selectionListener = listener;
	}

	/**
	 * Gets the package manager, which contains data for classes and nodes in the tree.
	 *
	 * @return the package manager
	 */
	public NestedPackages getPackageManager() {
		return this.packageManager;
	}

	/**
	 * Clears all classes in the tree, and replaces them with the given list of classes. If the list is null, no entries are added back.
	 *
	 * @param classEntries the list of classes to display
	 */
	public void setClasses(@Nullable Collection<ClassEntry> classEntries) {
		List<StateEntry> state = this.getExpansionState();

		if (classEntries == null) {
			this.setModel(null);
			this.packageManager = null;
			return;
		}

		// update the tree control
		this.packageManager = new NestedPackages(classEntries, this.comparator, this.controller.getProject().getMapper());
		this.setModel(new DefaultTreeModel(this.packageManager.getRoot()));
		this.invalidateStats();

		this.restoreExpansionState(state);
	}

	/**
	 * Gets the deobfuscated version of the currently selected class.
	 *
	 * <p> The deobfuscated class entry provides name information. For renaming, use {@link #getSelectedClassObf()}.
	 *
	 * @return the deobfuscated class entry
	 * @see #getSelectedClassObf()
	 */
	public ClassEntry getSelectedClassDeobf() {
		return this.getSelectedClass(false);
	}

	/**
	 * Gets the obfuscated version of the currently selected class.
	 *
	 * <p> The obfuscated class entry can be used for renaming actions, but only provides the obfuscated name. For the mapped name, see {@link #getSelectedClassDeobf()}.
	 *
	 * @return the obfuscated class entry
	 * @see #getSelectedClassDeobf()
	 */
	public ClassEntry getSelectedClassObf() {
		return this.getSelectedClass(true);
	}

	private ClassEntry getSelectedClass(boolean obfuscated) {
		if (!this.isSelectionEmpty() && this.getSelectionPath() != null) {
			Object selectedNode = this.getSelectionPath().getLastPathComponent();

			if (selectedNode instanceof ClassSelectorClassNode classNode) {
				return obfuscated ? classNode.getObfEntry() : classNode.getDeobfEntry();
			}
		}

		return null;
	}

	public enum State {
		EXPANDED,
		SELECTED
	}

	public record StateEntry(State state, TreePath path) {
	}

	/**
	 * Gets the current expansion state of the tree, as a list of {@link StateEntry} objects.
	 *
	 * @return a list of {@link StateEntry} objects, with an entry for each expanded or selected node.
	 */
	public List<StateEntry> getExpansionState() {
		List<StateEntry> state = new ArrayList<>();
		int rowCount = this.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			TreePath path = this.getPathForRow(i);
			if (this.isPathSelected(path)) {
				state.add(new StateEntry(State.SELECTED, path));
			}

			if (this.isExpanded(path)) {
				state.add(new StateEntry(State.EXPANDED, path));
			}
		}

		return state;
	}

	/**
	 * Restores the expansion state from the given list. Does not clear current expansion state, instead adding onto it.
	 *
	 * @param expansionState a list of entries to restore
	 */
	public void restoreExpansionState(List<StateEntry> expansionState) {
		for (StateEntry entry : expansionState) {
			if (entry.state() == State.EXPANDED) {
				this.expandPath(entry.path());
			} else if (entry.state() == State.SELECTED) {
				this.addSelectionPath(entry.path());
			}
		}
	}

	/**
	 * Expands the package that matches the provided name.
	 * @param packageName the package name to expand
	 */
	public void expandPackage(String packageName) {
		if (packageName == null) {
			return;
		}

		this.expandPath(this.packageManager.getPackagePath(packageName));
	}

	/**
	 * Expands every package in the tree.
	 */
	public void expandAll() {
		for (DefaultMutableTreeNode packageNode : this.packageManager.getPackageNodes()) {
			this.expandPath(new TreePath(packageNode.getPath()));
		}
	}

	/**
	 * Collapses every package in the tree.
	 */
	public void collapseAll() {
		for (DefaultMutableTreeNode packageNode : this.packageManager.getPackageNodes()) {
			this.collapsePath(new TreePath(packageNode.getPath()));
		}
	}

	/**
	 * Sets the currently selected class and scrolls to it. Expands packages to ensure the class is visible.
	 *
	 * @param classEntry the class to select
	 */
	public void setSelectionClass(ClassEntry classEntry) {
		this.expandPackage(classEntry.getPackageName());
		ClassSelectorClassNode node = this.packageManager.getClassNode(classEntry);

		if (node != null) {
			TreePath path = new TreePath(node.getPath());
			this.setSelectionPath(path);
			this.scrollPathToVisible(path);
		}
	}

	/**
	 * Moves the entry into the tree, removing it and re-adding it if it already exists. Does not update the tree visually!
	 *
	 * @param classEntry the entry to add
	 */
	public void moveClassIn(ClassEntry classEntry) {
		this.removeEntry(classEntry);
		this.packageManager.addEntry(classEntry);
	}

	/**
	 * Removes the given class entry from the tree. Does not update the tree visually!
	 *
	 * @param classEntry the class to be removed
	 */
	public void removeEntry(ClassEntry classEntry) {
		this.packageManager.removeClassNode(classEntry);
	}

	/**
	 * Reloads the tree below the given node.
	 *
	 * @param node the node to be reloaded below
	 * @param instant whether the action should happen immediately
	 * @apiNote the {@code instant} parameter exists in case you need to restore state after a reload: if you attempt a reload and subsequent
	 * state restoration it's possible the reload will occur after the restoration and therefore be reset. Otherwise, it's encouraged to leave
	 * this false to avoid the possibility of concurrency issues.
	 */
	public void reload(SortedMutableTreeNode node, boolean instant) {
		DefaultTreeModel model = (DefaultTreeModel) this.getModel();
		if (model != null) {
			if (instant) {
				model.reload(node);
			} else {
				SwingUtilities.invokeLater(() -> model.reload(node));
			}
		}
	}

	/**
	 * Reloads the tree from the root node instantly.
	 */
	public void reload() {
		this.reload(this.packageManager.getRoot(), true);
	}

	/**
	 * Invalidates the stats for all classes in the tree, forcing them to be reloaded.
	 * Stats will be calculated asynchronously for each entry the next time that entry is visible.
	 */
	public void invalidateStats() {
		this.statsManager.invalidateStats();
	}

	/**
	 * Requests an asynchronous reload of the stats for the given class.
	 * On completion, the class's stats icon will be updated.
	 *
	 * @param classEntry the class to reload stats for
	 */
	public void reloadStats(ClassEntry classEntry) {
		ClassSelectorClassNode node = this.packageManager.getClassNode(classEntry);
		if (node != null) {
			node.reloadStats(this.controller.getGui(), this, true);
		}
	}

	public interface ClassSelectionListener {
		void onSelectClass(ClassEntry classEntry);
	}
}

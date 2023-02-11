/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.node.ClassSelectorClassNode;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class ClassSelector extends JTree {
	public static final Comparator<ClassEntry> DEOBF_CLASS_COMPARATOR = Comparator.comparing(ClassEntry::getFullName);

	private final Comparator<ClassEntry> comparator;
	private final GuiController controller;

	private NestedPackages packageManager;
	private ClassSelectionListener selectionListener;
	private RenameSelectionListener renameSelectionListener;

	public ClassSelector(Gui gui, Comparator<ClassEntry> comparator, boolean isRenamable) {
		this.comparator = comparator;
		this.controller = gui.getController();

		// configure the tree control
		this.setEditable(true);
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

		final DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			{
				this.setLeafIcon(GuiUtil.CLASS_ICON);
			}

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

				if (gui.getController().project != null && leaf && value instanceof ClassSelectorClassNode node) {
					this.setIcon(GuiUtil.getClassIcon(gui, node.getObfEntry()));
				}

				return this;
			}
		};
		this.setCellRenderer(renderer);

		final JTree tree = this;

		final DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree, renderer) {
			@Override
			public boolean isCellEditable(EventObject event) {
				return isRenamable && !(event instanceof MouseEvent) && super.isCellEditable(event);
			}
		};
		this.setCellEditor(editor);
		editor.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent e) {
				String data = editor.getCellEditorValue().toString();
				TreePath path = ClassSelector.this.getSelectionPath();

				Object realPath = path.getLastPathComponent();
				if (realPath instanceof DefaultMutableTreeNode node && data != null) {
					TreeNode parentNode = node.getParent();
					if (parentNode == null)
						return;
					boolean allowEdit = true;
					for (int i = 0; i < parentNode.getChildCount(); i++) {
						TreeNode childNode = parentNode.getChildAt(i);
						if (childNode != null && childNode.toString().equals(data) && childNode != node) {
							allowEdit = false;
							break;
						}
					}
					if (allowEdit && ClassSelector.this.renameSelectionListener != null) {
						Object prevData = node.getUserObject();
						Object objectData = node.getUserObject() instanceof ClassEntry ? new ClassEntry(((ClassEntry) prevData).getPackageName() + "/" + data) : data;

						ValidationContext context = new ValidationContext(null);
						ClassSelector.this.renameSelectionListener.onSelectionRename(context, node.getUserObject(), objectData, node);
						if (context.canProceed()) {
							node.setUserObject(objectData); // Make sure that it's modified
						} else {
							editor.cancelCellEditing();
						}
					} else {
						editor.cancelCellEditing();
					}
				}
			}

			@Override
			public void editingCanceled(ChangeEvent e) {
				// NOP
			}
		});
		// init defaults
		this.selectionListener = null;
		this.renameSelectionListener = null;
	}

	public void setSelectionListener(ClassSelectionListener val) {
		this.selectionListener = val;
	}

	public void setRenameSelectionListener(RenameSelectionListener renameSelectionListener) {
		this.renameSelectionListener = renameSelectionListener;
	}

	public NestedPackages getPackageManager() {
		return this.packageManager;
	}

	public void setClasses(Collection<ClassEntry> classEntries) {
		List<StateEntry> state = this.getExpansionState();

		if (classEntries == null) {
			this.setModel(null);
			this.packageManager = null;
			return;
		}

		// update the tree control
		this.packageManager = new NestedPackages(classEntries, this.comparator, this.controller.project.getMapper());
		this.setModel(new DefaultTreeModel(this.packageManager.getRoot()));

		this.restoreExpansionState(state);
	}

	public ClassEntry getSelectedClass() {
		if (!this.isSelectionEmpty()) {
			Object selectedNode = this.getSelectionPath().getLastPathComponent();

			if (selectedNode instanceof ClassSelectorClassNode classNode) {
				return classNode.getClassEntry();
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

	public void restoreExpansionState(List<StateEntry> expansionState) {
		this.clearSelection();

		for (StateEntry entry : expansionState) {
			if (entry.state() == State.EXPANDED) {
				this.expandPath(entry.path());
			} else if (entry.state() == State.SELECTED) {
				this.addSelectionPath(entry.path());
			}
		}
	}

	public void expandPackage(String packageName) {
		if (packageName == null) {
			return;
		}

		this.expandPath(this.packageManager.getPackagePath(packageName));
	}

	public void expandAll() {
		for (DefaultMutableTreeNode packageNode : this.packageManager.getPackageNodes()) {
			this.expandPath(new TreePath(packageNode.getPath()));
		}
	}

	public void collapseAll() {
		for (DefaultMutableTreeNode packageNode : this.packageManager.getPackageNodes()) {
			this.collapsePath(new TreePath(packageNode.getPath()));
		}
	}

	public void setSelectionClass(ClassEntry classEntry) {
		this.expandPackage(classEntry.getPackageName());
		ClassSelectorClassNode node = this.packageManager.getClassNode(classEntry);

		if (node != null) {
			TreePath path = new TreePath(node.getPath());
			this.setSelectionPath(path);
			this.scrollPathToVisible(path);
		}
	}

	public void moveClassIn(ClassEntry classEntry) {
		this.removeEntry(classEntry);
		this.packageManager.addEntry(classEntry);
	}

	public void removeEntry(ClassEntry classEntry) {
		this.packageManager.removeClassNode(classEntry);
	}

	public void reload() {
		DefaultTreeModel model = (DefaultTreeModel) this.getModel();
		model.reload(this.packageManager.getRoot());
	}

	public interface ClassSelectionListener {
		void onSelectClass(ClassEntry classEntry);
	}

	public interface RenameSelectionListener {
		void onSelectionRename(ValidationContext vc, Object prevData, Object data, DefaultMutableTreeNode node);
	}
}

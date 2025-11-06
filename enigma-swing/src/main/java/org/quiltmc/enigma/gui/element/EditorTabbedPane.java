package org.quiltmc.enigma.gui.element;

import com.google.common.collect.HashBiMap;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.event.EditorActionListener;
import org.quiltmc.enigma.gui.panel.ClosableTabTitlePane;
import org.quiltmc.enigma.gui.panel.EditorPanel;
import org.quiltmc.enigma.gui.util.GuiUtil;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import static javax.swing.SwingUtilities.getUIInputMap;
import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;

public class EditorTabbedPane {
	private final JTabbedPane openFiles = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	private final HashBiMap<ClassEntry, EditorPanel> editors = HashBiMap.create();

	private final EditorTabPopupMenu editorTabPopupMenu;
	private final Gui gui;
	private NavigatorPanel navigator;

	public EditorTabbedPane(Gui gui) {
		this.gui = gui;
		this.editorTabPopupMenu = new EditorTabPopupMenu(this);
		this.navigator = new NavigatorPanel(this.gui);

		this.openFiles.addMouseListener(GuiUtil.onMousePress(this::onTabPressed));
		final InputMap openFilesInputMap = getUIInputMap(this.openFiles, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		if (openFilesInputMap != null) {
			// remove default JTabbedPane binding that conflicts with KeyBind for editors
			openFilesInputMap.remove(KeyBinds.ENTRY_NAVIGATOR_LAST.toKeyStroke());
		}
	}

	public EditorPanel openClass(ClassEntry entry) {
		EditorPanel activeEditor = this.getActiveEditor();
		EditorPanel entryEditor = this.editors.computeIfAbsent(entry, editing -> {
			ClassHandle classHandle = this.gui.getController().getClassHandleProvider().openClass(editing);
			if (classHandle == null) {
				return null;
			}

			this.navigator = new NavigatorPanel(this.gui);
			EditorPanel newEditor = new EditorPanel(this.gui, this.navigator);
			newEditor.setClassHandle(classHandle);
			this.openFiles.addTab(newEditor.getSimpleClassName(), newEditor.getUi());

			ClosableTabTitlePane titlePane = new ClosableTabTitlePane(newEditor.getSimpleClassName(), newEditor.getFullClassName(), () -> this.closeEditor(newEditor));
			this.openFiles.setTabComponentAt(this.openFiles.indexOfComponent(newEditor.getUi()), titlePane.getUi());
			titlePane.setTabbedPane(this.openFiles);

			newEditor.addListener(new EditorActionListener() {
				@Override
				public void onCursorReferenceChanged(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
					if (editor == EditorTabbedPane.this.getActiveEditor()) {
						EditorTabbedPane.this.gui.showCursorReference(ref);
					}
				}

				@Override
				public void onClassHandleChanged(EditorPanel editor, ClassEntry old, ClassHandle ch) {
					EditorTabbedPane.this.editors.remove(old);
					EditorTabbedPane.this.editors.put(ch.getRef(), editor);
				}

				@Override
				public void onTitleChanged(EditorPanel editor, String title) {
					titlePane.setText(editor.getSimpleClassName(), editor.getFullClassName());
				}
			});

			putKeyBindAction(KeyBinds.EDITOR_CLOSE_TAB, newEditor.getEditor(), e -> this.closeEditor(newEditor));
			putKeyBindAction(KeyBinds.ENTRY_NAVIGATOR_NEXT, newEditor.getEditor(), e -> newEditor.getNavigatorPanel().navigateDown());
			putKeyBindAction(KeyBinds.ENTRY_NAVIGATOR_LAST, newEditor.getEditor(), e -> newEditor.getNavigatorPanel().navigateUp());

			return newEditor;
		});

		if (entryEditor != null && activeEditor != entryEditor) {
			this.openFiles.setSelectedComponent(this.editors.get(entry).getUi());
			this.gui.updateStructure(entryEditor);
			this.gui.showCursorReference(entryEditor.getCursorReference());
		}

		return entryEditor;
	}

	public void closeEditor(EditorPanel ed) {
		this.openFiles.remove(ed.getUi());
		this.editors.inverse().remove(ed);
		EditorPanel activeEditor = this.getActiveEditor();
		if (activeEditor != null) {
			activeEditor.getEditor().requestFocus();
		}

		this.gui.updateStructure(activeEditor);
		this.gui.showCursorReference(activeEditor != null ? activeEditor.getCursorReference() : null);
		ed.destroy();
	}

	public void closeAllEditorTabs() {
		for (Iterator<EditorPanel> iter = this.editors.values().iterator(); iter.hasNext(); ) {
			EditorPanel e = iter.next();
			this.openFiles.remove(e.getUi());
			e.destroy();
			iter.remove();
		}
	}

	public void closeTabsLeftOf(EditorPanel ed) {
		int index = this.openFiles.indexOfComponent(ed.getUi());

		for (int i = index - 1; i >= 0; i--) {
			this.closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		}
	}

	public void closeTabsRightOf(EditorPanel ed) {
		int index = this.openFiles.indexOfComponent(ed.getUi());

		for (int i = this.openFiles.getTabCount() - 1; i > index; i--) {
			this.closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		}
	}

	public void closeTabsExcept(EditorPanel ed) {
		int index = this.openFiles.indexOfComponent(ed.getUi());

		for (int i = this.openFiles.getTabCount() - 1; i >= 0; i--) {
			if (i == index) continue;
			this.closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		}
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return EditorPanel.byUi(this.openFiles.getSelectedComponent());
	}

	private void onTabPressed(MouseEvent e) {
		int i = this.openFiles.getUI().tabForCoordinate(this.openFiles, e.getX(), e.getY());

		if (i != -1) {
			if (SwingUtilities.isRightMouseButton(e)) {
				this.editorTabPopupMenu.show(this.openFiles, e.getX(), e.getY(), EditorPanel.byUi(this.openFiles.getComponentAt(i)));
			}

			EditorPanel activeEditor = this.getActiveEditor();
			activeEditor.getEditor().requestFocus();
			this.gui.updateStructure(activeEditor);
			this.gui.showCursorReference(activeEditor != null ? activeEditor.getCursorReference() : null);
		}
	}

	public void retranslateUi() {
		this.editorTabPopupMenu.retranslateUi();
		this.editors.values().forEach(EditorPanel::retranslateUi);
	}

	public Component getUi() {
		return this.openFiles;
	}

	public void reloadKeyBinds() {
		this.editors.values().forEach(EditorPanel::reloadKeyBinds);
	}
}

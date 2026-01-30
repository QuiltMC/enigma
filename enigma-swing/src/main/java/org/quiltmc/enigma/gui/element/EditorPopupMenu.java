package org.quiltmc.enigma.gui.element;

import com.google.common.collect.ImmutableMap;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.gui.config.keybind.KeyBind;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.docker.StructureDocker;
import org.quiltmc.enigma.gui.panel.EditorPanel;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.Map;

public class EditorPopupMenu {
	private final JPopupMenu ui = new JPopupMenu();

	private final JMenuItem renameItem = new JMenuItem();
	private final JMenuItem pasteItem = new JMenuItem();
	private final JMenuItem editJavadocItem = new JMenuItem();
	private final JMenuItem showStructureItem = new JMenuItem();
	private final JMenuItem searchStructureItem = new JMenuItem();
	private final JMenuItem showInheritanceItem = new JMenuItem();
	private final JMenuItem showImplementationsItem = new JMenuItem();
	private final JMenuItem showCallsItem = new JMenuItem();
	private final JMenuItem showCallsSpecificItem = new JMenuItem();
	private final JMenuItem openEntryItem = new JMenuItem();
	private final JMenuItem openPreviousItem = new JMenuItem();
	private final JMenuItem openNextItem = new JMenuItem();
	private final JMenuItem toggleMappingItem = new JMenuItem();
	private final JMenuItem zoomInItem = new JMenuItem();
	private final JMenuItem zoomOutMenu = new JMenuItem();
	private final JMenuItem resetZoomItem = new JMenuItem();

	private final ImmutableMap<KeyBind, JMenuItem> buttonKeyBinds = ImmutableMap.ofEntries(
			Map.entry(KeyBinds.EDITOR_SHOW_INHERITANCE, this.showInheritanceItem),
			Map.entry(KeyBinds.EDITOR_SHOW_IMPLEMENTATIONS, this.showImplementationsItem),
			Map.entry(KeyBinds.EDITOR_OPEN_ENTRY, this.openEntryItem),
			Map.entry(KeyBinds.EDITOR_OPEN_PREVIOUS, this.openPreviousItem),
			Map.entry(KeyBinds.EDITOR_OPEN_NEXT, this.openNextItem),
			Map.entry(KeyBinds.EDITOR_SHOW_CALLS_SPECIFIC, this.showCallsSpecificItem),
			Map.entry(KeyBinds.EDITOR_SHOW_CALLS, this.showCallsItem),
			Map.entry(KeyBinds.EDITOR_TOGGLE_MAPPING, this.toggleMappingItem),
			Map.entry(KeyBinds.EDITOR_RENAME, this.renameItem),
			Map.entry(KeyBinds.EDITOR_EDIT_JAVADOC, this.editJavadocItem),
			Map.entry(KeyBinds.EDITOR_PASTE, this.pasteItem),
			Map.entry(KeyBinds.EDITOR_SEARCH_STRUCTURE, this.searchStructureItem),
			Map.entry(KeyBinds.EDITOR_SHOW_STRUCTURE, this.showStructureItem)
	);

	private final EditorPanel editor;
	private final Gui gui;

	public EditorPopupMenu(EditorPanel editor, Gui gui) {
		this.editor = editor;
		this.gui = gui;

		this.retranslateUi();

		this.ui.add(this.renameItem);
		this.ui.add(this.pasteItem);
		this.ui.add(this.editJavadocItem);
		this.ui.add(this.showInheritanceItem);
		this.ui.add(this.showImplementationsItem);
		this.ui.add(this.showCallsItem);
		this.ui.add(this.showCallsSpecificItem);
		this.ui.add(this.showStructureItem);
		this.ui.add(this.searchStructureItem);
		this.ui.add(this.openEntryItem);
		this.ui.add(this.openPreviousItem);
		this.ui.add(this.openNextItem);
		this.ui.add(this.toggleMappingItem);
		this.ui.addSeparator();
		this.ui.add(this.zoomInItem);
		this.ui.add(this.zoomOutMenu);
		this.ui.add(this.resetZoomItem);

		this.renameItem.setEnabled(false);
		this.pasteItem.setEnabled(false);
		this.editJavadocItem.setEnabled(false);
		this.showInheritanceItem.setEnabled(false);
		this.showImplementationsItem.setEnabled(false);
		this.showCallsItem.setEnabled(false);
		this.showCallsSpecificItem.setEnabled(false);
		this.openEntryItem.setEnabled(false);
		this.openPreviousItem.setEnabled(false);
		this.openNextItem.setEnabled(false);
		this.toggleMappingItem.setEnabled(false);

		this.renameItem.addActionListener(event -> gui.startRename(editor));
		this.pasteItem.addActionListener(event -> gui.startRename(editor, GuiUtil.getClipboard()));
		this.editJavadocItem.addActionListener(event -> gui.startDocChange(editor));
		this.showStructureItem.addActionListener(event -> {
			gui.openDocker(StructureDocker.class);
			gui.getDockerManager().getDocker(StructureDocker.class).focusTree();
		});
		this.searchStructureItem.addActionListener(event -> {
			gui.openDocker(StructureDocker.class);
			gui.getDockerManager().getDocker(StructureDocker.class).focusSearch();
		});
		this.showInheritanceItem.addActionListener(event -> gui.showInheritance(editor));
		this.showImplementationsItem.addActionListener(event -> gui.showImplementations(editor));
		this.showCallsItem.addActionListener(event -> gui.showCalls(editor, true));
		this.showCallsSpecificItem.addActionListener(event -> gui.showCalls(editor, false));
		this.openEntryItem.addActionListener(event -> gui.getController().navigateTo(editor.getCursorReference().entry));
		this.openPreviousItem.addActionListener(event -> gui.getController().openPreviousReference());
		this.openNextItem.addActionListener(event -> gui.getController().openNextReference());
		this.toggleMappingItem.addActionListener(event -> gui.toggleMapping(editor));
		this.zoomInItem.addActionListener(event -> editor.offsetEditorZoom(2));
		this.zoomOutMenu.addActionListener(event -> editor.offsetEditorZoom(-2));
		this.resetZoomItem.addActionListener(event -> editor.resetEditorZoom());
	}

	public ImmutableMap<KeyBind, ? extends AbstractButton> getButtonKeyBinds() {
		return this.buttonKeyBinds;
	}

	public void updateUiState() {
		EntryReference<Entry<?>, Entry<?>> reference = this.editor.getCursorReference();
		Entry<?> referenceEntry = reference == null ? null : reference.entry;
		GuiController controller = this.gui.getController();

		boolean isClassEntry = referenceEntry instanceof ClassEntry;
		boolean isFieldEntry = referenceEntry instanceof FieldEntry;
		boolean isMethodEntry = referenceEntry instanceof MethodEntry me && !me.isConstructor();
		boolean isConstructorEntry = referenceEntry instanceof MethodEntry me && me.isConstructor();
		boolean isRenamable = reference != null && controller.getProject().isRenamable(reference);

		EditableType type = EditableType.fromEntry(referenceEntry);

		this.renameItem.setEnabled(isRenamable && (type != null && this.gui.isEditable(type)));
		this.pasteItem.setEnabled(isRenamable && (type != null && this.gui.isEditable(type)));
		this.editJavadocItem.setEnabled(isRenamable && this.gui.isEditable(EditableType.JAVADOC));
		this.showInheritanceItem.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
		this.showImplementationsItem.setEnabled(isClassEntry || isMethodEntry);
		this.showCallsItem.setEnabled(isRenamable && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.showCallsSpecificItem.setEnabled(isRenamable && isMethodEntry);
		this.openEntryItem.setEnabled(isRenamable && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.openPreviousItem.setEnabled(controller.hasPreviousReference());
		this.openNextItem.setEnabled(controller.hasNextReference());
		this.toggleMappingItem.setEnabled(isRenamable && (type != null && this.gui.isEditable(type)));

		this.translateToggleMappingItem(reference);
	}

	public void retranslateUi() {
		this.renameItem.setText(I18n.translate("popup_menu.rename"));
		this.pasteItem.setText(I18n.translate("popup_menu.paste"));
		this.editJavadocItem.setText(I18n.translate("popup_menu.javadoc"));
		this.showInheritanceItem.setText(I18n.translate("popup_menu.inheritance"));
		this.showImplementationsItem.setText(I18n.translate("popup_menu.implementations"));
		this.showCallsItem.setText(I18n.translate("popup_menu.calls"));
		this.showCallsSpecificItem.setText(I18n.translate("popup_menu.calls.specific"));
		this.showStructureItem.setText(I18n.translate("popup_menu.structure"));
		this.searchStructureItem.setText(I18n.translate("popup_menu.search_structure"));
		this.openEntryItem.setText(I18n.translate("popup_menu.declaration"));
		this.openPreviousItem.setText(I18n.translate("popup_menu.back"));
		this.openNextItem.setText(I18n.translate("popup_menu.forward"));
		this.zoomInItem.setText(I18n.translate("popup_menu.zoom.in"));
		this.zoomOutMenu.setText(I18n.translate("popup_menu.zoom.out"));
		this.resetZoomItem.setText(I18n.translate("popup_menu.zoom.reset"));

		this.translateToggleMappingItem(this.editor.getCursorReference());
	}

	private void translateToggleMappingItem(EntryReference<Entry<?>, Entry<?>> reference) {
		if (
				reference != null && this.gui
					.getController().getProject().getRemapper()
					.extendedDeobfuscate(reference.getNameableEntry())
					.getType() == TokenType.DEOBFUSCATED
		) {
			this.toggleMappingItem.setText(I18n.translate("popup_menu.reset_obfuscated"));
		} else {
			this.toggleMappingItem.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		}
	}

	public JPopupMenu getUi() {
		return this.ui;
	}
}

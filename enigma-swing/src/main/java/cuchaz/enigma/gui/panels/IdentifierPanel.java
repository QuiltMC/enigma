package cuchaz.enigma.gui.panels;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.gui.EditableType;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.ConvertingTextField;
import cuchaz.enigma.gui.events.ConvertingTextFieldListener;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ValidationContext;

public class IdentifierPanel {

	private final Gui gui;

	private final JPanel ui = new JPanel();

	private Entry<?> entry;
	private Entry<?> deobfEntry;

	private ConvertingTextField nameField;

	private final ValidationContext vc = new ValidationContext();

	public IdentifierPanel(Gui gui) {
		this.gui = gui;

		this.ui.setLayout(new GridBagLayout());
		this.ui.setPreferredSize(ScaleUtil.getDimension(0, 150));
		this.ui.setBorder(BorderFactory.createTitledBorder(I18n.translate("info_panel.identifier")));
		this.ui.setEnabled(false);
	}

	public void setReference(Entry<?> entry) {
		this.entry = entry;
		this.refreshReference();
	}

	public boolean startRenaming() {
		if (this.nameField == null) return false;

		this.nameField.startEditing();

		return true;
	}

	public boolean startRenaming(String text) {
		if (this.nameField == null) return false;

		this.nameField.startEditing();
		this.nameField.setEditText(text);

		return true;
	}

	private void onModifierChanged(AccessModifier modifier) {
		this.gui.validateImmediateAction(vc -> this.gui.getController().applyChange(vc, EntryChange.modify(this.entry).withAccess(modifier)));
	}

	public void refreshReference() {
		this.deobfEntry = this.entry == null ? null : this.gui.getController().project.getMapper().deobfuscate(this.entry);

		this.nameField = null;

		TableHelper th = new TableHelper(this.ui, this.entry, this.gui);
		th.begin();
		if (this.entry == null) {
			this.ui.setEnabled(false);
		} else {
			this.ui.setEnabled(true);

			if (this.deobfEntry instanceof ClassEntry ce) {
				String name = ce.isInnerClass() ? ce.getName() : ce.getFullName();
				this.nameField = th.addRenameTextField(EditableType.CLASS, name);
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), this.entry.getName());
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.CLASS, this::onModifierChanged);
			} else if (this.deobfEntry instanceof FieldEntry fe) {
				this.nameField = th.addRenameTextField(EditableType.FIELD, fe.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), fe.getParent().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), this.entry.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type_descriptor"), fe.getDesc().toString());
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.FIELD, this::onModifierChanged);
			} else if (this.deobfEntry instanceof MethodEntry me) {
				if (me.isConstructor()) {
					ClassEntry ce = me.getParent();
					if (ce != null) {
						String name = ce.isInnerClass() ? ce.getName() : ce.getFullName();
						this.nameField = th.addRenameTextField(EditableType.CLASS, name);
					}
				} else {
					this.nameField = th.addRenameTextField(EditableType.METHOD, me.getName());
					th.addStringRow(I18n.translate("info_panel.identifier.class"), me.getParent().getFullName());
				}
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), this.entry.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.method_descriptor"), me.getDesc().toString());
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.METHOD, this::onModifierChanged);
			} else if (this.deobfEntry instanceof LocalVariableEntry lve) {
				EditableType type;

				if (lve.isArgument()) {
					type = EditableType.PARAMETER;
				} else {
					type = EditableType.LOCAL_VARIABLE;
				}

				this.nameField = th.addRenameTextField(type, lve.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), lve.getContainingClass().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.method"), lve.getParent().getName());
				th.addStringRow(I18n.translate("info_panel.identifier.index"), Integer.toString(lve.getIndex()));
			} else {
				throw new IllegalStateException("unreachable");
			}
		}
		th.end();

		if (this.nameField != null) {
			this.nameField.addListener(new ConvertingTextFieldListener() {
				@Override
				public void onStartEditing(ConvertingTextField field) {
					int i = field.getText().lastIndexOf('/');
					if (i != -1) {
						field.selectSubstring(i + 1);
					}
				}

				@Override
				public boolean tryStopEditing(ConvertingTextField field, boolean abort) {
					if (abort) return true;
					IdentifierPanel.this.vc.reset();
					IdentifierPanel.this.vc.setActiveElement(field);
					IdentifierPanel.this.validateRename(field.getText());
					return IdentifierPanel.this.vc.canProceed();
				}

				@Override
				public void onStopEditing(ConvertingTextField field, boolean abort) {
					if (!abort) {
						IdentifierPanel.this.vc.reset();
						IdentifierPanel.this.vc.setActiveElement(field);
						IdentifierPanel.this.doRename(field.getText());
					}

					EditorPanel e = IdentifierPanel.this.gui.getActiveEditor();
					if (e != null) {
						e.getEditor().requestFocusInWindow();
					}
				}
			});
		}

		this.ui.validate();
		this.ui.repaint();
	}

	private void validateRename(String newName) {
		this.gui.getController().validateChange(this.vc, this.getRename(newName));
	}

	private void doRename(String newName) {
		this.gui.getController().applyChange(this.vc, this.getRename(newName));
	}

	private EntryChange<? extends Entry<?>> getRename(String newName) {
		Entry<?> entry = this.entry;
		if (entry instanceof MethodEntry method && method.isConstructor()) {
			entry = method.getContainingClass();
		}

		return EntryChange.modify(entry).withDeobfName(newName);
	}

	public void retranslateUi() {
		this.ui.setBorder(BorderFactory.createTitledBorder(I18n.translate("info_panel.identifier")));
		this.refreshReference();
	}

	public JPanel getUi() {
		return this.ui;
	}

	private static final class TableHelper {

		private final Container c;
		private final Entry<?> e;
		private final Gui gui;
		private int row;

		public TableHelper(Container c, Entry<?> e, Gui gui) {
			this.c = c;
			this.e = e;
			this.gui = gui;
		}

		public void begin() {
			this.c.removeAll();
			this.c.setLayout(new GridBagLayout());
		}

		public void addRow(Component c1, Component c2) {
			GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
					.insets(2)
					.anchor(GridBagConstraints.WEST);
			this.c.add(c1, cb.pos(0, this.row).build());
			this.c.add(c2, cb.pos(1, this.row).weightX(1.0).fill(GridBagConstraints.HORIZONTAL).build());

			this.row += 1;
		}

		public void addCopiableRow(JLabel c1, JLabel c2) {
			c2.addMouseListener(GuiUtil.onMouseClick(e -> {
				if (e.getButton() == MouseEvent.BUTTON1) {
					GuiUtil.copyToClipboard(c2.getText());
					GuiUtil.showPopup(c2, I18n.translate("popup.copied"), e.getXOnScreen(), e.getYOnScreen());
				}
			}));
			this.addRow(c1, c2);
		}

		public ConvertingTextField addConvertingTextField(String c1, String c2) {
			ConvertingTextField textField = new ConvertingTextField(c2);
			this.addRow(new JLabel(c1), textField.getUi());
			return textField;
		}

		public ConvertingTextField addRenameTextField(EditableType type, String c2) {
			String description = switch(type) {
				case CLASS -> I18n.translate("info_panel.identifier.class");
				case METHOD -> I18n.translate("info_panel.identifier.method");
				case FIELD -> I18n.translate("info_panel.identifier.field");
				case PARAMETER, LOCAL_VARIABLE -> I18n.translate("info_panel.identifier.variable");
				default -> throw new IllegalStateException("Unexpected value: " + type);
			};

			if (this.gui.getController().project.isRenamable(this.e)) {
				ConvertingTextField field = this.addConvertingTextField(description, c2);
				field.setEditable(this.gui.isEditable(type));
				return field;
			} else {
				this.addStringRow(description, c2);
				return null;
			}
		}

		public void addStringRow(String c1, String c2) {
			this.addRow(new JLabel(c1), GuiUtil.unboldLabel(new JLabel(c2)));
		}

		public void addCopiableStringRow(String c1, String c2) {
			this.addCopiableRow(new JLabel(c1), GuiUtil.unboldLabel(new JLabel(c2)));
		}

		public JComboBox<AccessModifier> addModifierRow(String c1, EditableType type, Consumer<AccessModifier> changeListener) {
			EnigmaProject project = this.gui.getController().project;

			if (!project.isRenamable(this.e)) {
				return null;
			}

			JComboBox<AccessModifier> combo = new JComboBox<>(AccessModifier.values());
			EntryMapping mapping = project.getMapper().getDeobfMapping(this.e);
			combo.setSelectedIndex(mapping.accessModifier().ordinal());

			if (this.gui.isEditable(type)) {
				combo.addItemListener(event -> {
					if (event.getStateChange() == ItemEvent.SELECTED) {
						AccessModifier modifier = (AccessModifier) event.getItem();
						changeListener.accept(modifier);
					}
				});
			} else {
				combo.setEnabled(false);
			}

			this.addRow(new JLabel(c1), combo);

			return combo;
		}

		public void end() {
			// Add an empty panel with y-weight=1 so that all the other elements get placed at the top edge
			this.c.add(new JPanel(), GridBagConstraintsBuilder.create().pos(0, this.row).weight(0.0, 1.0).build());
		}

	}

}

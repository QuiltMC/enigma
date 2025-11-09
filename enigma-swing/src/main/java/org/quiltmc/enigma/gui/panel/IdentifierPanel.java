package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.ConvertingTextField;
import org.quiltmc.enigma.gui.event.ConvertingTextFieldListener;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class IdentifierPanel {
	private final Gui gui;

	private final JPanel ui = new JPanel();

	private Entry<?> lastEntry;
	private Entry<?> entry;
	private Entry<?> deobfEntry;

	private ConvertingTextField nameField;

	private final ValidationContext vc;

	public IdentifierPanel(Gui gui) {
		this.gui = gui;
		this.vc = new ValidationContext(this.gui.getNotificationManager());

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

	public void refreshReference() {
		final EnigmaProject project = this.gui.getController().getProject();
		this.deobfEntry = this.entry == null ? null : project.getRemapper().deobfuscate(this.entry);

		// Prevent IdentifierPanel from being rebuilt if you didn't click off.
		if (this.lastEntry == this.entry && this.nameField != null) {
			if (!this.nameField.hasChanges()) {
				final String name;

				// Find what to set the name to.
				if (this.deobfEntry instanceof MethodEntry methodEntry && methodEntry.isConstructor()) {
					// Get the parent of the method if it is a constructor.
					final ClassEntry parent = methodEntry.getParent();

					if (parent == null) {
						throw new IllegalStateException("constructor method entry to render has no parent!");
					}

					name = parent.isInnerClass() ? parent.getName() : parent.getFullName();
				} else if (this.deobfEntry instanceof ClassEntry classEntry && !classEntry.isInnerClass()) {
					name = classEntry.getFullName();
				} else {
					name = this.deobfEntry.getName();
				}

				this.nameField.setReferenceText(name);
			}

			return;
		}

		this.lastEntry = this.entry;

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

				if (ce.getParent() != null) {
					th.addCopiableStringRow(I18n.translate("info_panel.identifier.outer_class"), ce.getParent().getFullName());
				}
			} else if (this.deobfEntry instanceof FieldEntry fe) {
				this.nameField = th.addRenameTextField(EditableType.FIELD, fe.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), fe.getParent().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), this.entry.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type"), toReadableType(fe.getDesc()));
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
			} else if (this.deobfEntry instanceof LocalVariableEntry local) {
				EditableType type;

				if (local.isArgument()) {
					type = EditableType.PARAMETER;
				} else {
					type = EditableType.LOCAL_VARIABLE;
				}

				this.nameField = th.addRenameTextField(type, local.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), local.getContainingClass().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.method"), local.getParent().getName());
				th.addStringRow(I18n.translate("info_panel.identifier.index"), Integer.toString(local.getIndex()));

				// type
				EntryIndex index = project.getJarIndex().getIndex(EntryIndex.class);
				// EntryIndex only contains obf entries, so use the obf entry to look up the local's descriptor
				@Nullable
				final LocalVariableDefEntry obfLocal = index.getDefinition((LocalVariableEntry) this.entry);
				final String localDesc = obfLocal == null
						? I18n.translate("info_panel.identifier.type.unknown")
						: toReadableType(project.getRemapper().deobfuscate(obfLocal.getDesc()));

				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type"), localDesc);
			} else {
				throw new IllegalStateException("unreachable");
			}

			var mapping = project.getRemapper().getMapping(this.entry);
			if (Config.main().development.showMappingSourcePlugin.value() && mapping.tokenType().isProposed()) {
				th.addStringRow(I18n.translate("dev.source_plugin"), mapping.sourcePluginId());
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

					IdentifierPanel.this.vc.setNotifier(IdentifierPanel.this.gui.getNotificationManager());
					IdentifierPanel.this.vc.reset();
					return IdentifierPanel.this.vc.canProceed();
				}

				@Override
				public void onStopEditing(ConvertingTextField field, boolean abort) {
					if (!abort) {
						IdentifierPanel.this.vc.setNotifier(IdentifierPanel.this.gui.getNotificationManager());
						IdentifierPanel.this.vc.reset();
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

	private static String toReadableType(TypeDescriptor descriptor) {
		var primitive = TypeDescriptor.Primitive.get(descriptor.toString().charAt(0));

		if (primitive != null) {
			return descriptor + " (" + primitive.getKeyword() + ")";
		} else {
			String raw = descriptor.toString();
			// type will look like "LClassName;", with an optional [ at the start to denote an array
			// strip semicolon (;) from the end
			raw = raw.substring(0, raw.length() - 1);
			// handle arrays: add "[]" to the end and strip "["
			while (raw.startsWith("[")) {
				raw = raw.substring(1) + "[]";
			}

			// strip "L"
			return raw.substring(1);
		}
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

		TableHelper(Container c, Entry<?> e, Gui gui) {
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
			c2.addMouseListener(GuiUtil.onMouseClick(event -> {
				if (event.getButton() == MouseEvent.BUTTON1) {
					GuiUtil.copyToClipboard(c2.getText());
					GuiUtil.showPopup(c2, I18n.translate("popup.copied"), event.getXOnScreen(), event.getYOnScreen());
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
			String description = switch (type) {
				case CLASS -> I18n.translate("info_panel.identifier.class");
				case METHOD -> I18n.translate("info_panel.identifier.method");
				case FIELD -> I18n.translate("info_panel.identifier.field");
				case PARAMETER, LOCAL_VARIABLE -> I18n.translate("info_panel.identifier.variable");
				default -> throw new IllegalStateException("Unexpected value: " + type);
			};

			if (this.gui.getController().getProject().isRenamable(this.e)) {
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

		public void end() {
			// Add an empty panel with y-weight=1 so that all the other elements get placed at the top edge
			this.c.add(new JPanel(), GridBagConstraintsBuilder.create().pos(0, this.row).weight(0.0, 1.0).build());
		}
	}
}

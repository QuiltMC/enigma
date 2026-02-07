package org.quiltmc.enigma.gui.panel;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.ConvertingTextField;
import org.quiltmc.enigma.gui.event.ConvertingTextFieldListener;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.ValidationContext;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

public class IdentifierPanel {
	private final Gui gui;

	private final JPanel ui = new JPanel();

	@Nullable
	private EntryReference<Entry<?>, Entry<?>> lastReference;
	@Nullable
	private EntryReference<Entry<?>, Entry<?>> reference;

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

	public void setReference(@Nullable EntryReference<Entry<?>, Entry<?>> reference) {
		this.reference = reference;
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

	private void refreshReference() {
		final EnigmaProject project = this.gui.getController().getProject();
		final Entry<?> obfEntry = this.reference == null ? null : this.reference.entry;
		final Entry<?> deobfEntry = obfEntry == null ? null : project.getRemapper().deobfuscate(obfEntry);

		// Prevent IdentifierPanel from being rebuilt if you didn't click off.
		if (this.lastReference == this.reference && this.nameField != null) {
			if (!this.nameField.hasChanges()) {
				// nameField != null => lastReference != null => reference != null => obfEntry != null =>
				assert deobfEntry != null;

				final String name;

				// Find what to set the name to.
				if (deobfEntry instanceof MethodEntry methodEntry && methodEntry.isConstructor()) {
					// Get the parent of the method if it is a constructor.
					final ClassEntry parent = methodEntry.getParent();

					if (parent == null) {
						throw new IllegalStateException("constructor method entry to render has no parent!");
					}

					// inner classes return their simple name, outer classes return their full name
					name = parent.getName();
				} else {
					name = deobfEntry.getName();
				}

				this.nameField.setReferenceText(name);
			}

			return;
		}

		this.lastReference = this.reference;

		this.nameField = null;

		TableHelper th = new TableHelper(this.ui, this.reference, this.gui);
		th.begin();
		if (obfEntry == null) {
			this.ui.setEnabled(false);
		} else {
			this.ui.setEnabled(true);

			if (deobfEntry instanceof ClassEntry clazz) {
				// inner classes return their simple name, outer classes return their full name
				this.nameField = th.addRenameTextField(EditableType.CLASS, clazz.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), obfEntry.getName());

				if (clazz.getParent() != null) {
					th.addCopiableStringRow(I18n.translate("info_panel.identifier.outer_class"), clazz.getParent().getFullName());
				}
			} else if (deobfEntry instanceof FieldEntry field) {
				this.nameField = th.addRenameTextField(EditableType.FIELD, field.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), field.getParent().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), obfEntry.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type"), toReadableType(field.getDesc()));
			} else if (deobfEntry instanceof MethodEntry method) {
				if (method.isConstructor()) {
					final ClassEntry parent = method.getParent();
					if (parent != null) {
						// inner classes return their simple name, outer classes return their full name
						this.nameField = th.addRenameTextField(EditableType.CLASS, parent.getName());
					}
				} else {
					this.nameField = th.addRenameTextField(EditableType.METHOD, method.getName());
					th.addStringRow(I18n.translate("info_panel.identifier.class"), method.getParent().getFullName());
				}

				th.addCopiableStringRow(I18n.translate("info_panel.identifier.obfuscated"), obfEntry.getName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.method_descriptor"), method.getDesc().toString());
			} else if (deobfEntry instanceof LocalVariableEntry local) {
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
				final EntryIndex index = project.getJarIndex().getIndex(EntryIndex.class);
				// EntryIndex only contains obf entries, so use the obf entry to look up the local's descriptor
				final LocalVariableDefEntry obfLocal = index.getDefinition((LocalVariableEntry) obfEntry);
				final String localDesc = obfLocal == null
						? I18n.translate("info_panel.identifier.type.unknown")
						: toReadableType(project.getRemapper().deobfuscate(obfLocal.getDesc()));

				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type"), localDesc);
			} else {
				throw new IllegalStateException("unreachable");
			}

			final EntryMapping mapping = project.getRemapper().getMapping(obfEntry);
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
		final Entry<?> entry = this.reference == null ? null : this.reference.getNameableEntry();

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
		private final Container container;
		private final EntryReference<Entry<?>, Entry<?>> reference;
		private final Gui gui;
		private int row;

		TableHelper(Container container, EntryReference<Entry<?>, Entry<?>> reference, Gui gui) {
			this.container = container;
			this.reference = reference;
			this.gui = gui;
		}

		public void begin() {
			this.container.removeAll();
			this.container.setLayout(new GridBagLayout());
		}

		public void addRow(Component c1, Component c2) {
			GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
					.insets(2)
					.anchor(GridBagConstraints.WEST);
			this.container.add(c1, cb.pos(0, this.row).build());
			this.container.add(c2, cb.pos(1, this.row).weightX(1.0).fill(GridBagConstraints.HORIZONTAL).build());

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

			if (this.reference != null && this.gui.getController().getProject().isRenamable(this.reference)) {
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
			this.container.add(new JPanel(), GridBagConstraintsBuilder.create().pos(0, this.row).weight(0.0, 1.0).build());
		}
	}
}

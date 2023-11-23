package org.quiltmc.enigma.gui.dialog;

import com.google.common.base.Strings;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.gui.GuiController;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTML;

public class JavadocDialog {
	private final JDialog ui;
	private final GuiController controller;
	private final Entry<?> entry;

	private final JTextArea text;

	private final ValidationContext vc;

	private JavadocDialog(JFrame parent, GuiController controller, Entry<?> entry, String preset) {
		this.ui = new JDialog(parent, I18n.translate("javadocs.edit"));
		this.controller = controller;
		this.entry = entry;
		this.text = new JTextArea(10, 40);
		this.vc = new ValidationContext(controller.getGui().getNotificationManager());

		// set up dialog
		Container contentPane = this.ui.getContentPane();
		contentPane.setLayout(new BorderLayout());

		// editor panel
		this.text.setText(preset);
		this.text.setTabSize(2);
		contentPane.add(new JScrollPane(this.text), BorderLayout.CENTER);
		this.text.addKeyListener(GuiUtil.onKeyPress(event -> {
			if (KeyBinds.DIALOG_SAVE.matches(event)) {
				if (event.isControlDown()) {
					this.doSave();
					if (this.vc.canProceed()) {
						this.close();
					}
				}
			} else if (KeyBinds.EXIT.matches(event)) {
				this.close();
			}
		}));
		this.text.setFont(Config.INSTANCE.getCurrentFonts().editor.value());

		// buttons panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("javadocs.instruction"))));
		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(event -> this.close());
		buttonsPanel.add(cancelButton);
		JButton saveButton = new JButton(I18n.translate("prompt.save"));
		saveButton.addActionListener(event -> this.doSave());
		buttonsPanel.add(saveButton);
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);

		// tags panel
		JMenuBar tagsMenu = new JMenuBar();

		// add javadoc tags
		for (JavadocTag tag : JavadocTag.values()) {
			JButton tagButton = new JButton(tag.getText());
			tagButton.addActionListener(action -> {
				boolean textSelected = this.text.getSelectedText() != null;
				String tagText = tag.isInline() ? "{" + tag.getText() + " }" : tag.getText() + " ";

				if (textSelected) {
					if (tag.isInline()) {
						tagText = "{" + tag.getText() + " " + this.text.getSelectedText() + "}";
					} else {
						tagText = tag.getText() + " " + this.text.getSelectedText();
					}

					this.text.replaceSelection(tagText);
				} else {
					this.text.insert(tagText, this.text.getCaretPosition());
				}

				if (tag.isInline()) {
					this.text.setCaretPosition(this.text.getCaretPosition() - 1);
				}

				this.text.grabFocus();
			});
			tagsMenu.add(tagButton);
		}

		// add html tags
		JComboBox<String> htmlList = new JComboBox<>();
		htmlList.setPreferredSize(new Dimension());
		for (HTML.Tag htmlTag : HTML.getAllTags()) {
			htmlList.addItem(htmlTag.toString());
		}

		htmlList.addActionListener(action -> {
			String tagText = "<" + htmlList.getSelectedItem().toString() + ">";
			this.text.insert(tagText, this.text.getCaretPosition());
			this.text.grabFocus();
		});
		tagsMenu.add(htmlList);

		contentPane.add(tagsMenu, BorderLayout.NORTH);

		// show the frame
		this.ui.setSize(ScaleUtil.getDimension(600, 400));
		this.ui.setLocationRelativeTo(parent);
		this.ui.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	// Called when the "Save" button gets clicked.
	public void doSave() {
		this.vc.reset();
		this.validate();
		if (!this.vc.canProceed()) return;
		this.save();
		if (!this.vc.canProceed()) return;
		this.close();
	}

	public void close() {
		this.ui.setVisible(false);
		this.ui.dispose();
	}

	public void validate() {
		this.controller.validateJavadocChange(this.vc, this.getEntryChange());
	}

	public void save() {
		this.controller.applyChange(this.vc, this.getEntryChange());
	}

	private EntryChange<?> getEntryChange() {
		return this.text.getText().isBlank() ? EntryChange.modify(this.entry).clearJavadoc() : EntryChange.modify(this.entry).withJavadoc(this.text.getText());
	}

	public static void show(JFrame parent, GuiController controller, EntryReference<Entry<?>, Entry<?>> entry) {
		// Get the existing text through the mapping as it works for all entries, including constructors.
		EntryMapping mapping = controller.getProject().getRemapper().getMapping(entry.entry);
		String text = Strings.nullToEmpty(mapping.javadoc());

		// Note: entry.entry is used instead of getNameableEntry() to include constructors,
		// which can be documented.
		JavadocDialog dialog = new JavadocDialog(parent, controller, entry.entry, text);
		dialog.ui.setVisible(true);
		dialog.text.grabFocus();
	}

	private enum JavadocTag {
		CODE(true),
		LINK(true),
		LINKPLAIN(true),
		VALUE(true),
		RETURN(false),
		SEE(false),
		THROWS(false);

		private final boolean inline;

		JavadocTag(boolean inline) {
			this.inline = inline;
		}

		public String getText() {
			return "@" + this.name().toLowerCase();
		}

		public boolean isInline() {
			return this.inline;
		}
	}
}
